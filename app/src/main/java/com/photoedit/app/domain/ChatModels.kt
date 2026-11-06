package com.photoedit.app.domain

enum class ChatProvider(val label: String, val namespace: String, val defaultModel: String) {
    OPENROUTER("OpenRouter", "openrouter_chat_private", "nvidia/nemotron-3.5-super-vl:free"),
    GROQ("Groq", "groq_chat_private", "openai/gpt-oss-20b"),
    CLOUDFLARE("Cloudflare Workers AI", "cloudflare_chat_private", "@cf/zai-org/glm-4.7-flash"),
    VENICE("Venice AI", "venice_chat_private", "venice-uncensored-1-2")
}

data class ChatModel(val id: String, val name: String, val free: Boolean? = null)

object ChatModels {
    fun presets(provider: ChatProvider): List<ChatModel> = when (provider) {
        ChatProvider.OPENROUTER -> listOf(
            ChatModel(provider.defaultModel, "NVIDIA Nemotron 3.5 Super VL", true),
            ChatModel("nvidia/nemotron-3.5-lightning:free", "NVIDIA Nemotron 3.5 Lightning", true)
        )
        ChatProvider.GROQ -> listOf(
            ChatModel(provider.defaultModel, "GPT-OSS 20B"),
            ChatModel("openai/gpt-oss-120b", "GPT-OSS 120B")
        ),
        ChatProvider.CLOUDFLARE -> listOf(
            ChatModel(provider.defaultModel, "GLM-4.7-Flash · Workers AI")
        )
        ChatProvider.VENICE -> listOf(
            ChatModel(provider.defaultModel, "Venice Uncensored 1.2")
        )
    }

    fun validate(provider: ChatProvider, model: String, allowPaid: Boolean) {
        require(model.length in 1..200 && model.matches(Regex("@?[A-Za-z0-9][A-Za-z0-9._:/-]*"))) {
            "请填写有效的模型 ID（不是网址），不能包含空格"
        }
        if (provider == ChatProvider.CLOUDFLARE) {
            require(model.startsWith("@cf/") && model.count { it == '/' } >= 2) { "Cloudflare 模型 ID 必须来自 Workers AI 模型目录" }
        }
        if (provider == ChatProvider.VENICE) {
            require(model.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:-]*"))) { "Venice 模型 ID 格式无效" }
        }
        if (provider == ChatProvider.OPENROUTER && !allowPaid) {
            require(model.endsWith(":free") || model == "openrouter/free") {
                "当前仅允许免费模型；请选择 :free 模型，或明确开启付费模型"
            }
        }
    }

    // Missing/invalid prices must never be presented as free.
    fun zeroPrice(value: String?): Boolean =
        value?.toBigDecimalOrNull()?.compareTo(java.math.BigDecimal.ZERO) == 0
}
