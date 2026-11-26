package com.photoedit.app.ai

import android.content.Context
import com.photoedit.app.domain.AiRules
import com.photoedit.app.domain.ChatProvider
import com.photoedit.app.domain.ChatModels
import com.photoedit.app.domain.ChatRules
import com.photoedit.app.domain.NovelMemory

data class ChatSettings(val token: String, val model: String, val account: String = AiRules.DEFAULT_ACCOUNT)

/** Active providers reuse their existing encrypted credential storage. */
class ChatSettingsStore(context: Context) {
    private val stores = ChatProvider.entries.associateWith { provider ->
        when (provider) {
            ChatProvider.CLOUDFLARE -> AiSettingsStore(context)
            ChatProvider.APIPOD -> AiSettingsStore(context, "apipod_private")
            ChatProvider.UNCENSOREDLLM -> AiSettingsStore(context, ChatProvider.UNCENSOREDLLM.namespace)
        }
    }
    private val prefs = context.getSharedPreferences("chat_selection", Context.MODE_PRIVATE)

    fun novelMemory() = NovelMemory(prefs.getString("novel_setting", "").orEmpty(), prefs.getString("novel_summary", "").orEmpty())

    fun saveNovelMemory(value: NovelMemory) {
        check(prefs.edit().putString("novel_setting", value.setting)
            .putString("novel_summary", value.summary).commit()) { "小说资料保存失败" }
    }

    fun promptRules(): String = prefs.getString("prompt_rules", null)?.takeIf { it.isNotBlank() } ?: ChatRules.SYSTEM_PROMPT

    fun savePromptRules(value: String) {
        require(value.isNotBlank() && value.length <= 8000) { "规则长度应为 1–8000 字符" }
        check(prefs.edit().putString("prompt_rules", value).commit()) { "规则保存失败" }
    }

    fun resetPromptRules() {
        check(prefs.edit().remove("prompt_rules").commit()) { "恢复默认规则失败" }
    }

    fun provider(): ChatProvider =
        prefs.getString("provider", ChatProvider.CLOUDFLARE.name)
            ?.let { runCatching { ChatProvider.valueOf(it) }.getOrNull() }
            ?: ChatProvider.CLOUDFLARE

    fun load(provider: ChatProvider): ChatSettings {
        val (account, token) = stores.getValue(provider).load()
        val savedModel = prefs.getString("model_${provider.name}", provider.defaultModel) ?: provider.defaultModel
        // Do not restore a retired preset from an older installation.
        val model = ChatModels.restoreModel(provider, savedModel)
        return ChatSettings(token, model, account)
    }

    fun select(provider: ChatProvider, model: String) {
        check(prefs.edit().putString("provider", provider.name)
            .putString("model_${provider.name}", model).commit()) { "模型设置保存失败" }
    }

    fun saveKey(provider: ChatProvider, token: String) {
        val account = if (provider == ChatProvider.CLOUDFLARE) stores.getValue(provider).load().first else AiRules.DEFAULT_ACCOUNT
        stores.getValue(provider).save(account, token)
    }

    fun clearKey(provider: ChatProvider) = stores.getValue(provider).clear()
}
