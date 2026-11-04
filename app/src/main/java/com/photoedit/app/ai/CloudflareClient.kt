package com.photoedit.app.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.photoedit.app.domain.AiRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CloudflareClient {
    suspend fun generate(account: String, token: String, prompt: String, reference: Bitmap?): Bitmap = withContext(Dispatchers.IO) {
        AiRules.validateCredentials(account, token)
        AiRules.validatePrompt(prompt)
        val boundary = "PhotoEdit" + UUID.randomUUID().toString()
        val body = ByteArrayOutputStream().apply {
            fun text(value: String) { write(value.toByteArray(Charsets.UTF_8)) }
            fun field(name: String, value: String) {
                text("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
            }
            field("prompt", prompt)
            field("width", "1024")
            field("height", "1024")
            if (reference != null) {
                val scale = minOf(1f, 511f / maxOf(reference.width, reference.height))
                val small = Bitmap.createScaledBitmap(reference, (reference.width * scale).toInt().coerceAtLeast(1),
                    (reference.height * scale).toInt().coerceAtLeast(1), true)
                try {
                    text("--$boundary\r\nContent-Disposition: form-data; name=\"input_image_0\"; filename=\"reference.png\"\r\nContent-Type: image/png\r\n\r\n")
                    check(small.compress(Bitmap.CompressFormat.PNG, 100, this)) { "参考图编码失败" }
                    text("\r\n")
                } finally { if (small !== reference) small.recycle() }
            }
            text("--$boundary--\r\n")
        }.toByteArray()
        val connection = URL("https://api.cloudflare.com/client/v4/accounts/$account/ai/run/${AiRules.MODEL}")
            .openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 180_000
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setFixedLengthStreamingMode(body.size)
            }
            connection.outputStream.use { it.write(body) }
            val http = connection.responseCode
            val data = (if (http in 200..299) connection.inputStream else connection.errorStream)
                ?.use { readLimited(it) } ?: throw IOException(AiRules.diagnosticMessage(http, null, null, token, account))
            val imageBytes = if (http in 200..299 && connection.contentType.orEmpty().startsWith("image/")) data else {
                val json = runCatching { JSONObject(data.toString(Charsets.UTF_8)) }.getOrNull()
                val error = json?.optJSONArray("errors")?.optJSONObject(0)
                val code = error?.optInt("code", -1)?.takeIf { it >= 0 }
                if (http !in 200..299 || json?.optBoolean("success", true) == false) {
                    throw IOException(AiRules.diagnosticMessage(http, code, error?.optString("message"), token, account))
                }
                val result = json?.opt("result")
                val encoded = when (result) {
                    is JSONObject -> result.optString("image")
                    is String -> result
                    else -> ""
                }
                if (encoded.isBlank()) throw IOException("平台没有返回图片，请检查模型响应或内容审核状态")
                Base64.decode(encoded.substringAfter("base64,", encoded), Base64.DEFAULT)
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
            require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096 &&
                bounds.outWidth.toLong() * bounds.outHeight <= 8_000_000L) { "返回图片尺寸异常" }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: throw IOException("生成图片解码失败")
        } finally { connection.disconnect() }
    }
    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > 24 * 1024 * 1024) throw IOException("平台响应过大，已停止读取")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
