package com.photoedit.app.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class RunwareClient {
    companion object { const val MODEL = "meta:muse@image" }
    suspend fun generate(key: String, prompt: String, reference: Bitmap?): Bitmap = withContext(Dispatchers.IO) {
        require(key.isNotBlank()) { "请先填写 Runware API Key" }
        require(prompt.isNotBlank()) { "请输入提示词" }
        val task = JSONObject().put("taskType", "imageInference")
            .put("taskUUID", UUID.randomUUID().toString())
            .put("model", MODEL).put("positivePrompt", prompt)
            .put("numberResults", 1).put("includeCost", true)
            .put("deliveryMethod", "sync")
            .put("settings", JSONObject().put("moderation", "low"))
        if (reference == null) {
            task.put("width", 1600).put("height", 1600)
        } else {
            val stream = ByteArrayOutputStream()
            val scale = minOf(1f, 1600f / maxOf(reference.width, reference.height))
            val scaled = Bitmap.createScaledBitmap(reference,
                (reference.width * scale).toInt().coerceAtLeast(1),
                (reference.height * scale).toInt().coerceAtLeast(1), true)
            try {
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, 88, stream)) throw IOException("参考图编码失败")
            } finally { if (scaled !== reference) scaled.recycle() }
            val uri = "data:image/jpeg;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            task.put("inputs", JSONObject().put("referenceImages", JSONArray().put(uri)))
        }
        val connection = URL("https://api.runware.ai/v1").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 180_000
            connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $key")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(JSONArray().put(task).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val bytes = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readBytes() } ?: throw IOException("Runware 无响应 (HTTP $status)")
            if (bytes.size > 2_000_000) throw IOException("Runware 响应过大")
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            val result = json.optJSONArray("data")?.optJSONObject(0)
            if (status !in 200..299 || result == null) {
                val detail = json.optJSONArray("errors")?.optJSONObject(0)?.optString("message")
                    ?: json.optJSONObject("error")?.optString("message") ?: "请检查模型、额度或审核规则"
                throw IOException("Runware HTTP $status: " + detail.replace(key, "[REDACTED]").take(240))
            }
            val imageUrl = result.optString("imageURL")
            if (!imageUrl.startsWith("https://")) throw IOException("Runware 未返回有效图片地址")
            val imageConnection = URL(imageUrl).openConnection() as HttpURLConnection
            try {
                imageConnection.connectTimeout = 30_000
                imageConnection.readTimeout = 60_000
                val imageBytes = imageConnection.inputStream.use { it.readBytes() }
                if (imageBytes.size > 24 * 1024 * 1024) throw IOException("图片过大")
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: throw IOException("图片解码失败")
            } finally { imageConnection.disconnect() }
        } finally { connection.disconnect() }
    }
}
