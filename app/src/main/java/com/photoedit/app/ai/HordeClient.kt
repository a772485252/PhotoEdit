package com.photoedit.app.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.photoedit.app.domain.AiRules
import com.photoedit.app.domain.HordeModel
import com.photoedit.app.domain.HordeRules
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
import java.net.URL
import java.util.UUID

class HordeTaskException(message: String, val terminal: Boolean = false) : IOException(message)

/** Async REST adapter. Poll an existing ID; never resubmit it on timeout or transport failure. */
class HordeClient {
    private val base = "https://aihorde.net/api/v2"

    suspend fun models(): List<HordeModel> = withContext(Dispatchers.IO) {
        val data = JSONArray(request("GET", "/status/models?type=image&min_count=1"))
        (0 until data.length()).mapNotNull { i ->
            val item = data.getJSONObject(i)
            val name = item.optString("name")
            val count = item.optInt("count")
            if (name.isBlank() || count < 1) null else HordeModel(name, count)
        }.sortedByDescending { it.workers }.take(100)
    }

    suspend fun submit(key: String, prompt: String, model: String, nsfw: Boolean, reference: Bitmap?, denoise: Double): String = withContext(Dispatchers.IO) {
        AiRules.validatePrompt(prompt)
        val profile = HordeRules.sampling(model)
        val payload = JSONObject().put("prompt", HordeRules.optimizedPrompt(prompt, model))
            .put("models", JSONArray().put(HordeRules.validateModel(model)))
            .put("params", JSONObject().put("width", 512).put("height", 512).put("steps", profile.steps)
                .put("n", 1).put("sampler_name", profile.sampler).put("cfg_scale", profile.cfg)
                .put("karras", profile.karras)
                .apply { if (reference != null) put("denoising_strength", HordeRules.denoise(denoise)) })
            .put("nsfw", nsfw).put("censor_nsfw", !nsfw)
            .put("trusted_workers", true).put("slow_workers", true)
            .put("r2", true).put("shared", false).put("allow_downgrade", false)
        if (reference != null) {
            // Fit, do not stretch or crop. The worker receives an aspect-correct source image.
            val scale = minOf(1f, 512f / maxOf(reference.width, reference.height))
            val scaled = Bitmap.createScaledBitmap(reference,
                (reference.width * scale).toInt().coerceAtLeast(1), (reference.height * scale).toInt().coerceAtLeast(1), true)
            val stream = ByteArrayOutputStream()
            try {
                @Suppress("DEPRECATION")
                val ok = scaled.compress(Bitmap.CompressFormat.WEBP, 100, stream)
                check(ok) { "参考图编码失败" }
            } finally { if (scaled !== reference) scaled.recycle() }
            payload.put("source_image", Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP))
                .put("source_processing", "img2img")
        }
        val response = JSONObject(request("POST", "/generate/async", key, payload))
        val id = response.optString("id")
        if (!validId(id)) throw HordeTaskException("提交响应缺少任务 ID，结果未知；请先在平台核对任务，勿反复提交。")
        id
    }

    suspend fun await(id: String, onProgress: (String) -> Unit): Bitmap = withContext(Dispatchers.IO) {
        require(validId(id))
        repeat(200) {
            currentCoroutineContext().ensureActive()
            val status = JSONObject(request("GET", "/generate/check/$id"))
            if (status.optBoolean("faulted")) throw HordeTaskException("AI Horde 任务失败；草稿已保留。", true)
            if (status.optBoolean("done")) return@withContext result(id)
            currentCoroutineContext().ensureActive()
            onProgress(HordeRules.queueMessage(status.optInt("processing"), status.optInt("queue_position", -1),
                status.optInt("wait_time", -1), status.optBoolean("is_possible", true)))
            delay(3_000)
        }
        throw HordeTaskException("等待已达约 10 分钟；任务状态未确认，点“继续查询”获取原任务，不会重复提交。")
    }

    suspend fun cancel(id: String) = withContext(Dispatchers.IO) {
        require(validId(id))
        request("DELETE", "/generate/status/$id")
        Unit
    }

    private fun result(id: String): Bitmap {
        val data = JSONObject(request("GET", "/generate/status/$id"))
        val image = data.optJSONArray("generations")?.optJSONObject(0)
            ?: throw HordeTaskException("任务结束但没有图片，可能被节点拒绝。", true)
        if (image.optBoolean("censored")) throw HordeTaskException("节点按内容规则拦截了结果；未显示占位图片。", true)
        val source = image.optString("img")
        val bytes = if (source.startsWith("https://")) {
            val url = URL(source)
            // Horde R2 results only; do not fetch arbitrary worker-controlled or local URLs.
            val host = url.host.lowercase()
            require(host.endsWith(".r2.dev") || host.endsWith(".r2.cloudflarestorage.com") || host == "aihorde.net" || host.endsWith(".aihorde.net")) {
                "AI Horde 返回了未识别的图片主机，已停止下载"
            }
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = false
                if (connection.responseCode !in 200..299) throw IOException("图片下载失败，可继续查询原任务")
                connection.inputStream.use { readLimited(it, 12 * 1024 * 1024) }
            } finally { connection.disconnect() }
        } else {
            if (source.length > 16 * 1024 * 1024) throw IOException("图片响应过大")
            try { Base64.decode(source, Base64.DEFAULT) }
            catch (_: IllegalArgumentException) { throw IOException("图片编码无效，可继续查询原任务") }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096) throw IOException("图片尺寸无效")
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("图片解码失败，可继续查询原任务")
    }

    private fun request(method: String, path: String, key: String? = null, body: JSONObject? = null): String {
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "PhotoEdit/0.7.1 (Android)")
            connection.setRequestProperty("Client-Agent", "PhotoEdit:0.7.1:https://github.com/a772485252/PhotoEdit")
            if (key != null) connection.setRequestProperty("apikey", HordeRules.apiKey(key))
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { readLimited(it, 2 * 1024 * 1024).toString(Charsets.UTF_8) }.orEmpty()
            if (code !in 200..299) {
                val message = when (code) {
                    401 -> "AI Horde Key 无效，请检查设置"
                    403 -> "AI Horde 拒绝请求，请检查账号 Kudos、参考图权限或内容规则；本次未自动降低画质"
                    404 -> "AI Horde 任务不存在或已过期"
                    429 -> "AI Horde 请求过于频繁，请稍后继续查询或重试"
                    400 -> "AI Horde 参数或模型不受支持，请刷新模型或调整提示词；部分请求需要 Kudos"
                    else -> "AI Horde 服务或网络异常（HTTP $code）；已有任务可继续查询"
                }
                throw HordeTaskException(message, code == 404)
            }
            return text
        } finally { connection.disconnect() }
    }

    private fun validId(id: String): Boolean = runCatching { UUID.fromString(id) }.isSuccess
    private fun readLimited(stream: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (out.size() + count > limit) throw IOException("AI Horde 响应过大")
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }
}
