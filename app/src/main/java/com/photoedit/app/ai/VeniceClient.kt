package com.photoedit.app.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.photoedit.app.domain.VeniceRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class VeniceException(message: String, val outcomeUnknown: Boolean) : IOException(message)
data class VeniceResult(val image: Bitmap, val blurred: Boolean)

class VeniceClient {
    suspend fun generate(key: String, prompt: String, reference: Bitmap?): VeniceResult = withContext(Dispatchers.IO) {
        VeniceRules.validateKey(key)
        VeniceRules.validatePrompt(prompt)
        val json = JSONObject().put("model", VeniceRules.model(reference != null)).put("prompt", prompt)
            .put("safe_mode", true).put("enhance_prompt", false)
        val path = if (reference == null) {
            json.put("width", 1024).put("height", 1024).put("format", "webp")
                .put("variants", 1).put("return_binary", true)
            "/image/generate"
        } else {
            VeniceRules.validateReference(reference.width, reference.height)
            val scale = minOf(1f, 1024f / maxOf(reference.width, reference.height))
            val width = (reference.width * scale).toInt().coerceAtLeast(1)
            val height = (reference.height * scale).toInt().coerceAtLeast(1)
            VeniceRules.validateReference(width, height)
            val scaled = Bitmap.createScaledBitmap(reference, width, height, true)
            val output = ByteArrayOutputStream()
            try { check(scaled.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Venice 参考图编码失败" } }
            finally { if (scaled !== reference) scaled.recycle() }
            require(output.size() <= 5 * 1024 * 1024) { "Venice 参考图过大，请先缩小图片" }
            json.put("image", Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP))
                .put("output_format", "webp")
            "/image/edit"
        }
        val body = json.toString().toByteArray(Charsets.UTF_8)
        val connection = URL("https://api.venice.ai/api/v1$path").openConnection() as HttpURLConnection
        var submissionStarted = false
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 180_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.setRequestProperty("Authorization", "Bearer $key")
            connection.setRequestProperty("User-Agent", "PhotoEdit/0.9.0 (Android)")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "image/webp, image/png, image/jpeg")
            connection.outputStream.use {
                submissionStarted = true
                it.write(body)
            }
            val http = connection.responseCode
            if (http !in 200..299) {
                // Never expose raw provider errors: they can echo input or credentials.
                throw VeniceException(VeniceRules.errorMessage(http), VeniceRules.outcomeUnknown(http))
            }
            if (connection.getHeaderField("x-venice-is-content-violation").equals("true", true)) {
                throw VeniceException("Venice 返回了内容审核提示，请调整输入；未自动重试", false)
            }
            if (!connection.contentType.orEmpty().substringBefore(';').trim().startsWith("image/")) {
                throw VeniceException("Venice 未返回预期图片，结果与计费未知，请先核对平台用量", true)
            }
            val bytes = connection.inputStream.use { readLimited(it, 24 * 1024 * 1024) }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth !in 1..8192 || bounds.outHeight !in 1..8192) throw IOException("返回图片尺寸无效")
            val options = BitmapFactory.Options().apply { inSampleSize = 1 }
            while (bounds.outWidth / options.inSampleSize > 2048 || bounds.outHeight / options.inSampleSize > 2048) options.inSampleSize *= 2
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: throw IOException("返回图片无法解码")
            VeniceResult(image, connection.getHeaderField("x-venice-is-blurred").equals("true", true))
        } catch (error: VeniceException) {
            throw error
        } catch (_: OutOfMemoryError) {
            throw VeniceException("Venice 图片处理内存不足；若已提交，请先核对平台用量，未自动重试", submissionStarted)
        } catch (_: IOException) {
            throw VeniceException(if (submissionStarted) "Venice 连接中断或等待超时，结果与计费未知；请先核对平台用量，未自动重试"
                else "无法连接 Venice，请检查网络；未提交生成请求", submissionStarted)
        } finally { connection.disconnect() }
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > limit) throw IOException("Venice 返回图片过大")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
