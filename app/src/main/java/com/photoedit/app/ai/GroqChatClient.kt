package com.photoedit.app.ai

import com.photoedit.app.domain.AiRules
import com.photoedit.app.domain.ChatMessage
import com.photoedit.app.domain.ChatRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GroqChatClient {
    suspend fun reply(token: String, messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        AiRules.validateCredentials(AiRules.DEFAULT_ACCOUNT, token)
        val body = JSONObject().put("model", ChatRules.MODEL).put("stream", false)
            .put("max_completion_tokens", 2048).put("reasoning_effort", "low")
            .put("messages", JSONArray().apply { messages.forEach {
                put(JSONObject().put("role", it.role).put("content", it.text))
            } }).toString().toByteArray(Charsets.UTF_8)
        val connection = URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"; connectTimeout = 30_000; readTimeout = 120_000
                doOutput = true; instanceFollowRedirects = false
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setFixedLengthStreamingMode(body.size)
            }
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status !in 200..299) throw IOException(when (status) {
                401, 403 -> "Groq 认证或访问被拒绝，请检查独立的 Groq API Key、账号权限和地区可用性"
                429 -> "Groq 免费额度或速率限制已达到，请稍后再试；没有调用 Cloudflare，也没有自动重试"
                400, 404 -> "Groq 拒绝请求或模型不可用，请检查平台模型状态"
                else -> "Groq 请求失败（HTTP $status），请稍后手动重试"
            })
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 512 * 1024) throw IOException("聊天响应过大")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val choice = JSONObject(bytes.toString(Charsets.UTF_8)).optJSONArray("choices")?.optJSONObject(0)
            val answer = choice?.optJSONObject("message")?.optString("content").orEmpty().trim()
            if (answer.isBlank()) throw IOException("Groq 没有返回正文，请缩短需求后重试")
            if (choice?.optString("finish_reason") == "length") throw IOException("回复达到长度限制，请缩短需求后重试；本次回复未加入对话")
            answer.take(8000)
        } finally { connection.disconnect() }
    }
}
