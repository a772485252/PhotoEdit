package com.photoedit.app.ai

import android.content.Context
import com.photoedit.app.domain.AiRules
import com.photoedit.app.domain.ChatProvider

data class ChatSettings(val token: String, val model: String)

/** Provider keys retain separate Keystore aliases; the existing Groq namespace is unchanged. */
class ChatSettingsStore(context: Context) {
    private val stores = ChatProvider.entries.associateWith { if (it == ChatProvider.CLOUDFLARE) AiSettingsStore(context) else AiSettingsStore(context, it.namespace) }
    private val prefs = context.getSharedPreferences("chat_selection", Context.MODE_PRIVATE)

    fun provider(): ChatProvider = ChatProvider.entries.firstOrNull {
        it.name == prefs.getString("provider", null)
    } ?: ChatProvider.OPENROUTER

    fun load(provider: ChatProvider): ChatSettings = ChatSettings(
        stores.getValue(provider).load().second,
        prefs.getString("model_${provider.name}", provider.defaultModel) ?: provider.defaultModel
    )

    fun select(provider: ChatProvider, model: String) {
        check(prefs.edit().putString("provider", provider.name)
            .putString("model_${provider.name}", model).commit()) { "模型设置保存失败" }
    }

    fun saveKey(provider: ChatProvider, token: String) =
        stores.getValue(provider).save(AiRules.DEFAULT_ACCOUNT, token)

    fun clearKey(provider: ChatProvider) = stores.getValue(provider).clear()
}
