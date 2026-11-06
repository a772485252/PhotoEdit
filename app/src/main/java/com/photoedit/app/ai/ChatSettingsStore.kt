package com.photoedit.app.ai

import android.content.Context
import com.photoedit.app.domain.AiRules
import com.photoedit.app.domain.ChatProvider

data class ChatSettings(val token: String, val model: String, val account: String = AiRules.DEFAULT_ACCOUNT)

/** Provider keys retain separate Keystore aliases; the existing Groq namespace is unchanged. */
class ChatSettingsStore(context: Context) {
    private val stores = ChatProvider.entries.associateWith { when (it) { ChatProvider.CLOUDFLARE -> AiSettingsStore(context); ChatProvider.VENICE -> AiSettingsStore(context, "venice_private"); ChatProvider.APIPOD -> AiSettingsStore(context, "apipod_private"); else -> AiSettingsStore(context, it.namespace) } }
    private val prefs = context.getSharedPreferences("chat_selection", Context.MODE_PRIVATE)

    fun provider(): ChatProvider =
        prefs.getString("provider", ChatProvider.CLOUDFLARE.name)
            ?.let { runCatching { ChatProvider.valueOf(it) }.getOrNull() }
            ?.takeIf { it == ChatProvider.CLOUDFLARE || it == ChatProvider.VENICE || it == ChatProvider.APIPOD }
            ?: ChatProvider.CLOUDFLARE

    fun load(provider: ChatProvider): ChatSettings {
        val (account, token) = stores.getValue(provider).load()
        return ChatSettings(token, prefs.getString("model_${provider.name}", provider.defaultModel) ?: provider.defaultModel, account)
    }

    fun select(provider: ChatProvider, model: String) {
        require(provider == ChatProvider.CLOUDFLARE || provider == ChatProvider.VENICE || provider == ChatProvider.APIPOD)
        check(prefs.edit().putString("provider", provider.name)
            .putString("model_${provider.name}", model).commit()) { "模型设置保存失败" }
    }

    fun saveKey(provider: ChatProvider, token: String) {
        val account = if (provider == ChatProvider.CLOUDFLARE) stores.getValue(provider).load().first else AiRules.DEFAULT_ACCOUNT
        stores.getValue(provider).save(account, token)
    }

    fun clearKey(provider: ChatProvider) = stores.getValue(provider).clear()
}
