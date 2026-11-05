package com.photoedit.app.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.photoedit.app.domain.ApiPodRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.UUID
import kotlin.random.Random

class ApiPodException(message: String, val definitive: Boolean = false, val terminal: Boolean = false) : IOException(message)
// Session memory only. Never log payload or key. A resumed create reuses both body and idempotency key.
class ApiPodOperation(val key: String, val prompt: String, val referenceUsed: Boolean, val payload: String,
                      val idempotencyKey: String = UUID.randomUUID().toString()) {
    var taskId: String? = null
}
data class ApiPodResult(val image: Bitmap, val costUsd: Double?)

class ApiPodClient {
    suspend fun prepare(key: String, prompt: String, reference: Bitmap?, pro: Boolean): ApiPodOperation = withContext(Dispatchers.IO) {
        ApiPodRules.validateKey(key)
        ApiPodRules.validatePrompt(prompt)
        val json = JSONObject().put("model", ApiPodRules.model(reference != null, pro)).put("prompt", prompt)
        if (reference == null) {
            json.put("aspect_ratio", "1:1").put("prompt_extend", false)
        } else {
            val scale = minOf(1f, 1024f / maxOf(reference.width, reference.height))
            val scaled = Bitmap.createScaledBitmap(reference, (reference.width * scale).toInt().coerceAtLeast(1),
                (reference.height * scale).toInt().coerceAtLeast(1), true)
            val stream = ByteArrayOutputStream()
            try { check(scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "APIPod 参考图编码失败" } }
            finally { if (scaled !== reference) scaled.recycle() }
            require(stream.size() <= 5 * 1024 * 1024) { "APIPod 参考图过大，请先压缩后再试" }
            json.put("image_urls", JSONArray().put("data:image/png;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)))
        }
        ApiPodOperation(key, prompt, reference != null, json.toString())
    }

    suspend fun run(operation: ApiPodOperation, onProgress: (String) -> Unit): ApiPodResult = withContext(Dispatchers.IO) {
        if (operation.taskId == null) {
            onProgress("APIPod 正在确认原提交…")
            val data = request("POST", "/images/generations", operation.key, operation.payload, operation.idempotencyKey)
            val id = data.optString("task_id")
            if (id.isBlank()) throw ApiPodException("提交未返回任务 ID，结果未知；继续查询会使用同一幂等操作，勿另建重复任务")
            operation.taskId = ApiPodRules.validateTaskId(id)
        }
        val id = ApiPodRules.validateTaskId(operation.taskId!!)
        repeat(120) { attempt ->
            currentCoroutineContext().ensureActive()
            val data = request("GET", "/images/status/$id", operation.key)
            when (data.optString("status")) {
                "completed" -> {
                    val first = data.optJSONArray("result")?.opt(0)
                    val url = when (first) { is String -> first; is JSONObject -> first.optString("url"); else -> "" }
                    if (url.isBlank()) throw ApiPodException("任务已完成但结果暂不可读取；请继续查询原任务")
                    val image = download(url)
                    val rawCost = data.optJSONObject("usage")?.optDouble("cost", Double.NaN) ?: Double.NaN
                    return@withContext ApiPodResult(image, rawCost.takeIf { it.isFinite() && it >= 0 })
                }
                "failed", "cancelled" -> {
                    val code = data.optString("error_code").replace(operation.key, "REDACTED").replace(Regex("[^A-Za-z0-9_.-]"), "").take(80)
                    val detail = if (code.isBlank()) "，请查看平台任务记录" else "（$code），请查看平台任务记录"
                    throw ApiPodException("APIPod 任务${if (data.optString("status") == "cancelled") "已取消" else "失败"}$detail", terminal = true)
                }
                "pending", "processing" -> {
                    val percent = data.optInt("progress", -1)
                    onProgress("APIPod " + (if (data.optString("status") == "pending") "排队中" else "正在生成") +
                        (if (percent in 0..100) " · 平台进度 $percent%" else "…"))
                }
                else -> throw ApiPodException("APIPod 返回了未知任务状态；原任务已保留")
            }
            delay(minOf(10_000L, 2_000L + attempt * 1_000L) + Random.nextLong(500))
        }
        throw ApiPodException("等待时间较长；可继续查询原任务，不会重新生成。平台任务可能仍运行并计费")
    }

    private fun request(method: String, path: String, key: String, body: String? = null, idempotency: String? = null): JSONObject {
        val connection = URL("https://api.apipod.ai/v1$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 45_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $key")
            connection.setRequestProperty("User-Agent", "PhotoEdit/0.8.0 (Android)")
            if (idempotency != null) connection.setRequestProperty("Idempotency-Key", idempotency)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val http = connection.responseCode
            val stream = if (http in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { readLimited(it, 2 * 1024 * 1024).toString(Charsets.UTF_8) }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()
            val code = json?.optJSONObject("error")?.optString("code")?.takeIf { it.isNotBlank() }
                ?: json?.optJSONObject("data")?.optString("error_code").orEmpty()
            if (http !in 200..299 || (json != null && json.optInt("code", 200) !in 200..299)) {
                val effective = if (http !in 200..299) http else json!!.optInt("code", 500)
                throw ApiPodException(ApiPodRules.errorMessage(effective, code),
                    definitive = method == "POST" && ApiPodRules.definitiveSubmissionFailure(effective, code))
            }
            return json?.optJSONObject("data") ?: throw ApiPodException("APIPod 响应不完整；原操作已保留，勿重复新建")
        } finally { connection.disconnect() }
    }

    private fun download(value: String): Bitmap {
        val url = URL(value)
        require(url.protocol == "https" && url.userInfo == null && url.port in listOf(-1, 443)) { "APIPod 返回了无效图片地址" }
        val addresses = InetAddress.getAllByName(url.host)
        require(addresses.isNotEmpty() && addresses.none { it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress || it.isSiteLocalAddress || it.isMulticastAddress }) {
            "APIPod 图片地址不可用于下载"
        }
        val connection = url.openConnection() as HttpURLConnection
        try {
            // Result CDN only: never forward APIPod credentials to it or follow redirects.
            connection.connectTimeout = 20_000
            connection.readTimeout = 45_000
            connection.instanceFollowRedirects = false
            if (connection.responseCode !in 200..299) throw IOException("APIPod 图片下载失败；请继续查询原任务")
            val bytes = connection.inputStream.use { readLimited(it, 24 * 1024 * 1024) }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth !in 1..8192 || bounds.outHeight !in 1..8192) throw IOException("APIPod 图片尺寸无效")
            val options = BitmapFactory.Options().apply { inSampleSize = 1 }
            while (bounds.outWidth / options.inSampleSize > 2048 || bounds.outHeight / options.inSampleSize > 2048) options.inSampleSize *= 2
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: throw IOException("APIPod 图片解码失败")
        } finally { connection.disconnect() }
    }
    private fun readLimited(stream: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (output.size() + count > limit) throw IOException("APIPod 响应过大")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
