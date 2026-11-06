package com.photoedit.app.ai

import com.photoedit.app.domain.AiRules
import com.photoedit.app.domain.ChatMessage
import com.photoedit.app.domain.ChatModel
import com.photoedit.app.domain.ChatModels
import com.photoedit.app.domain.ChatProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Fixed platform endpoints only. Never follows redirects or logs provider responses/credentials. */
class ChatClient {
    suspend fun reply(provider: ChatProvider, token: String, model: String,
        messages: List<ChatMessage>, allowPaid: Boolean = false): String = withContext(Dispatchers.IO) {
        AiRules.validateCredentials(AiRules.DEFAULT_ACCOUNT, token)
        ChatModels.validate(provider, model, allowPaid)
        val body = JSONObject().put("messages", JSONArray().apply { messages.forEach {
            put(JSONObject().put("role", it.role).put("content", it.text))
        } })
        if (provider != ChatProvider.CLOUDFLARE) body.put("model", model).put("stream", false)
        if (provider == ChatProvider.GROQ) {
            body.put("max_completion_tokens", 2048)
            if (model.startsWith("openai/gpt-oss-")) body.put("reasoning_effort", "low")
        } else if (provider == ChatProvider.OPENROUTER) {
            body.put("max_tokens", 2048)
            if (!allowPaid) body.put("provider", JSONObject().put("max_price",
                JSONObject().put("prompt", 0).put("completion", 0).put("request", 0)))
        }
        val endpoint = if (provider == ChatProvider.CLOUDFLARE) "/ai/run/$model" else "/chat/completions"
        if (provider == ChatProvider.CLOUDFLARE) body.put("max_tokens", 2048)
        val result = request(provider, token, endpoint, body.toString().toByteArray(Charsets.UTF_8))
        if (provider == ChatProvider.CLOUDFLARE) {
            val answer = result.optJSONObject("result")?.optString("response").orEmpty().trim()
            if (answer.isBlank()) throw IOException("Cloudflare Workers AI 没有返回正文，请检查模型权限和额度")
            return@withContext answer.take(8000)
        }
        val choice = result.optJSONArray("choices")?.optJSONObject(0)
        val answer = choice?.optJSONObject("message")?.optString("content").orEmpty().trim()
        if (answer.isBlank()) throw IOException("${provider.label} 没有返回正文，请缩短需求或手动更换模型")
        if (choice?.optString("finish_reason") == "length")
            throw IOException("回复达到长度限制，请缩短需求后重试；本次回复未加入对话")
        answer.take(8000)
    }

    suspend fun openRouterModels(token: String): List<ChatModel> = withContext(Dispatchers.IO) {
        AiRules.validateCredentials(AiRules.DEFAULT_ACCOUNT, token)
        val data = request(ChatProvider.OPENROUTER, token, "/models", null, 8 * 1024 * 1024)
            .optJSONArray("data") ?: throw IOException("模型列表格式无效")
        buildList {
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val architecture = item.optJSONObject("architecture") ?: continue
                val inputs = architecture.optJSONArray("input_modalities") ?: continue
                val outputs = architecture.optJSONArray("output_modalities") ?: continue
                if ((0 until inputs.length()).none { inputs.optString(it) == "text" } ||
                    (0 until outputs.length()).none { outputs.optString(it) == "text" }) continue
                val endpoints = item.optJSONArray("endpoints")
                if (endpoints != null && (0 until endpoints.length()).none {
                    endpoints.optString(it) == "/api/v1/chat/completions"
                }) continue
                if (id.isBlank() || id.length > 200) continue
                val prices = item.optJSONObject("pricing")
                val free = prices != null && ChatModels.zeroPrice(prices.optString("prompt")) &&
                    ChatModels.zeroPrice(prices.optString("completion")) &&
                    ChatModels.zeroPrice(prices.optString("request", "0"))
                add(ChatModel(id, item.optString("name", id).take(160), free))
            }
        }.distinctBy { it.id }.sortedWith(compareBy<ChatModel> { !it.id.endsWith(":free") }.thenBy { it.name })
    }

    private fun request(provider: ChatProvider, token: String, path: String, body: ByteArray?,
        limit: Int = 512 * 1024): JSONObject {
        val base = when (provider) {
            ChatProvider.GROQ -> "https://api.groq.com/openai/v1"
            ChatProvider.OPENROUTER -> "https://openrouter.ai/api/v1"
            ChatProvider.CLOUDFLARE -> "https://api.cloudflare.com/client/v4/accounts/${AiRules.DEFAULT_ACCOUNT}"
        }
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = if (body == null) "GET" else "POST"
                connectTimeout = 30_000; readTimeout = 120_000
                instanceFollowRedirects = false
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (body != null) { doOutput = true; setFixedLengthStreamingMode(body.size) }
            }
            if (body != null) connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status !in 200..299) throw IOException(if (provider == ChatProvider.CLOUDFLARE) when (status) {
                401, 403 -> "Cloudflare API Token 无效或缺少 Workers AI 权限（需要 Workers AI Read 和 Edit）"
                429 -> "Cloudflare Workers AI 请求受限或免费额度已用完，请查看 AI 控制台"
                else -> "Cloudflare Workers AI 请求失败（HTTP $status），请检查 Account ID、Token 权限和模型可用性"
            } else when (status) {
                401 -> "${provider.label} Key 无效或已过期，请检查当前平台的 Key"
                402 -> "${provider.label} 余额或消费上限不足；可手动选择免费模型"
                403 -> "${provider.label} 拒绝访问，请检查账号、地区、隐私设置及内容要求"
                429 -> "${provider.label} 额度或速率受限，请稍后再试；未自动重试或切换平台"
                400, 404 -> "${provider.label} 请求或模型不可用，请刷新列表或检查模型 ID"
                else -> "${provider.label} 请求失败（HTTP $status），请稍后手动重试"
            })
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > limit) throw IOException("平台响应过大，请稍后重试")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val result = JSONObject(bytes.toString(Charsets.UTF_8))
            if (result.has("error") || result.optBoolean("success", true).not()) {
                val detail = result.optJSONArray("errors")?.optJSONObject(0)?.optString("message").orEmpty()
                throw IOException(if (provider == ChatProvider.CLOUDFLARE) "Cloudflare Workers AI 返回错误：${detail.take(180)}" else "${provider.label} 返回错误，请检查模型、额度或访问设置")
            }
            return result
        } finally { connection.disconnect() }
    }
}
