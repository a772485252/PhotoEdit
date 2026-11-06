package com.photoedit.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photoedit.app.ai.ChatClient
import com.photoedit.app.ai.ChatSettings
import com.photoedit.app.ai.ChatSettingsStore
import com.photoedit.app.domain.ChatMessage
import com.photoedit.app.domain.ChatModel
import com.photoedit.app.domain.ChatModels
import com.photoedit.app.domain.ChatProvider
import com.photoedit.app.domain.ChatRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class ChatUiState(
    val provider: ChatProvider = ChatProvider.CLOUDFLARE,
    val model: String = ChatProvider.CLOUDFLARE.defaultModel,
    val modelDraft: String = "",
    val models: List<ChatModel> = ChatModels.presets(ChatProvider.CLOUDFLARE),
    val allowPaid: Boolean = false,
    val token: String = "", val input: String = "", val promptMode: Boolean = true,
    val messages: List<ChatMessage> = emptyList(), val busy: Boolean = true, val notice: String? = null,
    val balanceInfo: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = ChatSettingsStore(application)
    private val client = ChatClient()
    private val drafts = mutableMapOf<ChatProvider, ChatSettings>()
    private val catalogs = mutableMapOf<ChatProvider, List<ChatModel>>()
    private val _state = MutableStateFlow(ChatUiState())
    val uiState = _state.asStateFlow()

    init {
        perform {
            val provider = withContext(Dispatchers.IO) { settings.provider() }
            _state.value = _state.value.copy(provider = provider, model = provider.defaultModel,
                models = ChatModels.presets(provider))
            val loaded = withContext(Dispatchers.IO) { settings.load(provider) }
            _state.value = _state.value.copy(token = loaded.token, model = loaded.model,
                balanceInfo = if (provider == ChatProvider.VENICE && loaded.token.isNotBlank())
                    runCatching { client.veniceBalance(loaded.token) }.getOrNull() else null)
        }
    }

    fun provider(value: ChatProvider) {
        val old = _state.value
        if (old.busy || old.provider == value) return
        drafts[old.provider] = ChatSettings(old.token, old.model)
        perform {
            val loaded = drafts[value] ?: withContext(Dispatchers.IO) { settings.load(value) }
            withContext(Dispatchers.IO) { settings.select(value, loaded.model) }
            val balance = if (value == ChatProvider.VENICE && loaded.token.isNotBlank())
                runCatching { client.veniceBalance(loaded.token) }.getOrNull() else null
            _state.value = _state.value.copy(provider = value, token = loaded.token, model = loaded.model,
                modelDraft = "", models = catalogs[value] ?: ChatModels.presets(value), allowPaid = false,
                balanceInfo = balance, messages = emptyList(), notice = if (value == ChatProvider.APIPOD) "已读取图片生成设置中的 APIPod Key；聊天与图片共用同一个 Key" else "已切换到 ${value.label}，开始新对话；各平台 Key 独立保存")
        }
    }

    fun modelDraft(value: String) {
        if (!_state.value.busy) _state.value = _state.value.copy(modelDraft = value.take(200))
    }

    fun model(value: String) {
        val state = _state.value
        if (state.busy) return
        perform {
            val id = value.trim()
            ChatModels.validate(state.provider, id, state.allowPaid)
            withContext(Dispatchers.IO) { settings.select(state.provider, id) }
            _state.value = _state.value.copy(model = id, modelDraft = "", messages = emptyList(),
                notice = "已选择模型并开始新对话")
        }
    }

    fun allowPaid(value: Boolean) {
        if (!_state.value.busy) _state.value = _state.value.copy(allowPaid = value)
    }

    fun refreshModels() {
        val state = _state.value
        if (state.busy || state.provider != ChatProvider.OPENROUTER) return
        perform {
            val models = client.openRouterModels(state.token)
            if (models.isEmpty()) throw java.io.IOException("没有返回可用的文字模型；保留原列表和当前选择")
            catalogs[state.provider] = models
            _state.value = _state.value.copy(models = models,
                notice = "模型列表已刷新；未发送聊天内容，也未更改当前模型")
        }
    }

    fun refreshBalance() {
        val state = _state.value
        if (state.busy || state.provider != ChatProvider.VENICE || state.token.isBlank()) return
        perform {
            val balance = client.veniceBalance(state.token)
            _state.value = _state.value.copy(balanceInfo = balance, notice = null)
        }
    }

    fun token(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(token = value.trim()) }
    fun input(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(input = value.take(2000)) }
    fun mode(value: Boolean) {
        if (!_state.value.busy && _state.value.promptMode != value)
            _state.value = _state.value.copy(promptMode = value, messages = emptyList(), notice = "已切换模式并开始新对话")
    }
    fun clearChat() { if (!_state.value.busy) _state.value = _state.value.copy(messages = emptyList(), notice = null) }
    fun saveKey() {
        val state = _state.value
        if (state.busy) return
        perform {
            withContext(Dispatchers.IO) { settings.saveKey(state.provider, state.token) }
            _state.value = _state.value.copy(notice = "${state.provider.label} Key 已加密保存在本机，尚未验证 API 权限")
        }
    }
    fun clearKey() {
        val state = _state.value
        if (state.busy) return
        perform {
            withContext(Dispatchers.IO) { settings.clearKey(state.provider) }
            drafts.remove(state.provider)
            _state.value = _state.value.copy(token = "", notice = "已清除 ${state.provider.label} Key；其他平台设置不受影响")
        }
    }
    fun send() {
        val state = _state.value
        if (state.busy) return
        perform {
            val request = ChatRules.messages(state.messages, state.input, state.promptMode)
            val answer = client.reply(state.provider, state.token, state.model, request, state.allowPaid)
            _state.value = _state.value.copy(input = "",
                messages = request.drop(1) + ChatMessage("assistant", answer), notice = null)
            if (state.provider == ChatProvider.VENICE) {
                val balance = runCatching { client.veniceBalance(state.token) }.getOrNull()
                _state.value = _state.value.copy(balanceInfo = balance)
            }
        }
    }
    fun regenerateLast() {
        val state = _state.value
        if (state.busy || state.messages.lastOrNull()?.role != "assistant") return
        val lastUserIndex = state.messages.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) return
        val lastUserText = state.messages[lastUserIndex].text
        val previousMessages = state.messages.take(lastUserIndex)
        perform {
            val request = ChatRules.messages(previousMessages, lastUserText, state.promptMode)
            val answer = client.reply(state.provider, state.token, state.model, request, state.allowPaid)
            _state.value = _state.value.copy(messages = request.drop(1) + ChatMessage("assistant", answer), notice = null)
            if (state.provider == ChatProvider.VENICE) {
                val balance = runCatching { client.veniceBalance(state.token) }.getOrNull()
                _state.value = _state.value.copy(balanceInfo = balance)
            }
        }
    }
    private fun perform(block: suspend () -> Unit) {
        _state.value = _state.value.copy(busy = true, notice = null)
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: SocketTimeoutException) { _state.value = _state.value.copy(notice = "${_state.value.provider.label} 响应超时，未自动重试；输入已保留") }
            catch (_: UnknownHostException) { _state.value = _state.value.copy(notice = "无法连接 ${_state.value.provider.label}，请检查网络；输入已保留") }
            catch (e: Exception) { _state.value = _state.value.copy(notice =
                if (e is IllegalArgumentException || e is java.io.IOException) e.message else "聊天操作失败，请检查设置后重试") }
            finally { _state.value = _state.value.copy(busy = false) }
        }
    }
}
