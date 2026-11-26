package com.photoedit.app.domain

enum class ChatProvider(val label: String, val namespace: String, val defaultModel: String) {
    CLOUDFLARE("Cloudflare Workers AI", "cloudflare_chat_private", "@cf/zai-org/glm-4.7-flash"),
    APIPOD("APIPod", "apipod_private", "grok-4.5"),
    UNCENSOREDLLM("UncensoredLLM", "uncensoredllm_chat_private", "glm-5.3-uncensored")
}

data class ChatModel(val id: String, val name: String)

object ChatModels {
    fun presets(provider: ChatProvider): List<ChatModel> = when (provider) {
        ChatProvider.UNCENSOREDLLM -> listOf(
            ChatModel(provider.defaultModel, "GLM 5.3 · Uncensored")
        )
        ChatProvider.CLOUDFLARE -> listOf(
            ChatModel(provider.defaultModel, "GLM-4.7-Flash · Workers AI")
        )
        ChatProvider.APIPOD -> listOf(
            ChatModel(provider.defaultModel, "Grok 4.5 · APIPod"),
            ChatModel("grok-4.6", "Grok 4.6 · APIPod")
        )
    }

    fun displayName(provider: ChatProvider, model: String): String = when (provider) {
        ChatProvider.CLOUDFLARE -> if (model == "@cf/zai-org/glm-4.7-flash") "GLM-4.7-Flash" else model.substringAfterLast('/')
        ChatProvider.APIPOD, ChatProvider.UNCENSOREDLLM -> presets(provider).firstOrNull { it.id == model }?.name ?: model
    }

    fun restoreModel(provider: ChatProvider, savedModel: String): String = when {
        provider == ChatProvider.APIPOD && savedModel == "deepseek-flash" -> provider.defaultModel
        provider == ChatProvider.UNCENSOREDLLM && savedModel == "glm-5.3-flash-uncensored" -> provider.defaultModel
        else -> savedModel
    }

    data class PriceGuide(
        val inputPerTenThousandChars: String,
        val outputPerTenThousandChars: String,
        val note: String
    )

    /**
     * Public list-price estimate for 10,000 Chinese characters, using 1 character ~= 1 token.
     * Actual tokenization, channel routing, context and output length change the final charge.
     */
    fun priceGuide(provider: ChatProvider, model: String): PriceGuide? = when {
        provider == ChatProvider.UNCENSOREDLLM && model == provider.defaultModel ->
            PriceGuide("\$0.0032", "\$0.0097",
                "按 1 字≈1 Token 粗估；2026-11-26 核对公开价输入 $0.3243、输出 $0.9729 / 百万 Token，实际以平台结算为准")
        provider == ChatProvider.CLOUDFLARE && model == "@cf/zai-org/glm-4.7-flash" ->
            PriceGuide("\$0.0006 · 约55 Neurons", "\$0.0040 · 约364 Neurons",
                "按 1 字≈1 Token 粗估；Workers AI 实际按 Neurons 计量")
        provider == ChatProvider.APIPOD && model == "grok-4.6" ->
            PriceGuide("\$0.0140", "\$0.0420",
                "按 1 字≈1 Token 粗估；APIPod xAI 渠道输入 \$1.40、输出 \$4.20 / 百万 Token，实际按 Token 结算")
        provider == ChatProvider.APIPOD && model == "grok-4.5" ->
            PriceGuide("\$0.002–\$0.014", "\$0.006–\$0.042",
                "按 1 字≈1 Token 粗估；区间对应 APIPod Grok Lite 至 xAI 默认渠道")
        else -> null
    }

    fun validate(provider: ChatProvider, model: String) {
        require(model.length in 1..200 && model.matches(Regex("@?[A-Za-z0-9][A-Za-z0-9._:/-]*"))) {
            "请填写有效的模型 ID（不是网址），不能包含空格"
        }
        if (provider == ChatProvider.CLOUDFLARE) {
            require(model.startsWith("@cf/") && model.count { it == '/' } >= 2) { "Cloudflare 模型 ID 必须来自 Workers AI 模型目录" }
        }
    }
}
