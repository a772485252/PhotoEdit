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
import com.photoedit.app.processing.PromptGenerationForegroundService
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
    val balanceInfo: String? = null, val balanceLoading: Boolean = false,
    val promptRules: String = ChatRules.SYSTEM_PROMPT
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
            val rules = withContext(Dispatchers.IO) { settings.promptRules() }
            _state.value = _state.value.copy(promptRules = rules)
            _state.value = _state.value.copy(provider = provider, model = provider.defaultModel,
                models = ChatModels.presets(provider))
            val loaded = withContext(Dispatchers.IO) { settings.load(provider) }
            _state.value = _state.value.copy(token = loaded.token, model = loaded.model)
            if (provider == ChatProvider.VENICE && loaded.token.isNotBlank()) refreshBalance()
        }
    }

    fun provider(value: ChatProvider) {
        val old = _state.value
        if (old.busy || old.provider == value) return
        drafts[old.provider] = ChatSettings(old.token, old.model)
        perform {
            val loaded = drafts[value] ?: withContext(Dispatchers.IO) { settings.load(value) }
            withContext(Dispatchers.IO) { settings.select(value, loaded.model) }
            _state.value = _state.value.copy(provider = value, token = loaded.token, model = loaded.model,
                modelDraft = "", models = catalogs[value] ?: ChatModels.presets(value), allowPaid = false,
                balanceInfo = null, balanceLoading = false, messages = emptyList(),
                notice = if (value == ChatProvider.APIPOD) "已读取图片生成设置中的 APIPod Key；聊天与图片共用同一个 Key" else "已切换到 ${value.label}，开始新对话；各平台 Key 独立保存")
            if (value == ChatProvider.VENICE && loaded.token.isNotBlank()) refreshBalance()
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
        val snapshot = _state.value
        if (snapshot.provider != ChatProvider.VENICE || snapshot.token.isBlank() || snapshot.balanceLoading) return
        _state.value = _state.value.copy(balanceLoading = true, balanceInfo = "正在查询余额…")
        viewModelScope.launch {
            try {
                val balance = withContext(Dispatchers.IO) { client.veniceBalance(snapshot.token) }
                val current = _state.value
                if (current.provider == snapshot.provider && current.token == snapshot.token)
                    _state.value = current.copy(balanceInfo = balance)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val current = _state.value
                if (current.provider == snapshot.provider && current.token == snapshot.token)
                    _state.value = current.copy(balanceInfo = "余额查询失败，请检查 Key 或稍后刷新")
            } finally {
                val current = _state.value
                if (current.provider == snapshot.provider && current.token == snapshot.token)
                    _state.value = current.copy(balanceLoading = false)
            }
        }
    }

    fun token(value: String) {
        if (!_state.value.busy) _state.value = _state.value.copy(token = value.trim(), balanceInfo = null)
    }
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
            if (state.provider == ChatProvider.VENICE) refreshBalance()
        }
    }
    fun clearKey() {
        val state = _state.value
        if (state.busy) return
        perform {
            withContext(Dispatchers.IO) { settings.clearKey(state.provider) }
            drafts.remove(state.provider)
            _state.value = _state.value.copy(token = "", balanceInfo = null, balanceLoading = false,
                notice = "已清除 ${state.provider.label} Key；其他平台设置不受影响")
        }
    }
    fun savePromptRules(value: String) {
        if (_state.value.busy) return
        perform {
            withContext(Dispatchers.IO) { settings.savePromptRules(value) }
            _state.value = _state.value.copy(promptRules = value, notice = "扩写规则已保存；聊天记录和输入草稿已保留，新规则从下一次提示词请求开始生效")
        }
    }
    fun resetPromptRules() {
        if (_state.value.busy) return
        perform {
            withContext(Dispatchers.IO) { settings.resetPromptRules() }
            _state.value = _state.value.copy(promptRules = ChatRules.SYSTEM_PROMPT, notice = "已恢复默认扩写规则；聊天记录和输入草稿已保留")
        }
    }
    fun send() {
        val state = _state.value
        if (state.busy) return
        perform(foreground = true) {
            val request = ChatRules.messages(state.messages, state.input, state.promptMode, state.promptRules)
            val activeToken = if (state.provider == ChatProvider.APIPOD) withContext(Dispatchers.IO) { settings.load(ChatProvider.APIPOD).token } else state.token
            if (state.provider == ChatProvider.APIPOD && activeToken.isBlank()) throw java.io.IOException("APIPod Key 尚未保存，请先在设置中保存 Key")
            if (state.provider == ChatProvider.APIPOD && activeToken != state.token) _state.value = _state.value.copy(token = activeToken)
            val answer = client.reply(state.provider, activeToken, state.model, request, state.allowPaid)
            _state.value = _state.value.copy(input = "",
                messages = request.drop(1) + ChatMessage("assistant", answer), notice = null)
            if (state.provider == ChatProvider.VENICE) refreshBalance()
        }
    }
    fun regenerateLast() {
        val state = _state.value
        if (state.busy || state.messages.lastOrNull()?.role != "assistant") return
        val lastUserIndex = state.messages.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) return
        val lastUserText = state.messages[lastUserIndex].text
        val previousMessages = state.messages.take(lastUserIndex)
        perform(foreground = true) {
            val request = ChatRules.messages(previousMessages, lastUserText, state.promptMode, state.promptRules)
            val activeToken = if (state.provider == ChatProvider.APIPOD) withContext(Dispatchers.IO) { settings.load(ChatProvider.APIPOD).token } else state.token
            if (state.provider == ChatProvider.APIPOD && activeToken.isBlank()) throw java.io.IOException("APIPod Key 尚未保存，请先在设置中保存 Key")
            if (state.provider == ChatProvider.APIPOD && activeToken != state.token) _state.value = _state.value.copy(token = activeToken)
            val answer = client.reply(state.provider, activeToken, state.model, request, state.allowPaid)
            _state.value = _state.value.copy(messages = request.drop(1) + ChatMessage("assistant", answer), notice = null)
            if (state.provider == ChatProvider.VENICE) refreshBalance()
        }
    }
    private fun perform(foreground: Boolean = false, block: suspend () -> Unit) {
        if (foreground) {
            try { PromptGenerationForegroundService.start(getApplication()) }
            catch (_: Exception) {
                _state.value = _state.value.copy(busy = false, notice = "无法启动后台生成服务，请保持应用在前台后重试")
                return
            }
        }
        _state.value = _state.value.copy(busy = true, notice = null)
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: SocketTimeoutException) { _state.value = _state.value.copy(notice = "${_state.value.provider.label} 响应超时，未自动重试；输入已保留") }
            catch (_: UnknownHostException) { _state.value = _state.value.copy(notice = "无法连接 ${_state.value.provider.label}，请检查网络；输入已保留") }
            catch (e: Exception) { _state.value = _state.value.copy(notice =
                if (e is IllegalArgumentException || e is java.io.IOException) e.message else "聊天操作失败，请检查设置后重试") }
            finally {
                _state.value = _state.value.copy(busy = false)
                if (foreground) PromptGenerationForegroundService.stop(getApplication())
            }
        }
    }
}
