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
import com.photoedit.app.domain.ChatMode
import com.photoedit.app.domain.NovelMemory
import com.photoedit.app.domain.NovelRules
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
    val token: String = "", val input: String = "", val mode: ChatMode = ChatMode.PROMPT,
    val messages: List<ChatMessage> = emptyList(), val busy: Boolean = true, val notice: String? = null,
    val promptRules: String = ChatRules.SYSTEM_PROMPT,
    val novelMemory: NovelMemory = NovelMemory()
) {
    val promptMode: Boolean get() = mode == ChatMode.PROMPT
    val novelMode: Boolean get() = mode == ChatMode.NOVEL
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = ChatSettingsStore(application)
    private val client = ChatClient()
    private val drafts = mutableMapOf<ChatProvider, ChatSettings>()
    private val _state = MutableStateFlow(ChatUiState())
    val uiState = _state.asStateFlow()

    init {
        perform {
            val provider = withContext(Dispatchers.IO) { settings.provider() }
            val rules = withContext(Dispatchers.IO) { settings.promptRules() }
            _state.value = _state.value.copy(promptRules = rules, novelMemory = withContext(Dispatchers.IO) { settings.novelMemory() })
            _state.value = _state.value.copy(provider = provider, model = provider.defaultModel,
                models = ChatModels.presets(provider))
            val loaded = withContext(Dispatchers.IO) { settings.load(provider) }
            _state.value = _state.value.copy(token = loaded.token, model = loaded.model)
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
                modelDraft = "", models = ChatModels.presets(value),
                messages = emptyList(),
                notice = if (value == ChatProvider.APIPOD) "已读取图片生成设置中的 APIPod Key；聊天与图片共用同一个 Key" else "已切换到 ${value.label}，开始新对话；各平台 Key 独立保存")
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
            ChatModels.validate(state.provider, id)
            withContext(Dispatchers.IO) { settings.select(state.provider, id) }
            _state.value = _state.value.copy(model = id, modelDraft = "", messages = emptyList(),
                notice = "已选择模型并开始新对话")
        }
    }

    fun token(value: String) {
        if (!_state.value.busy) _state.value = _state.value.copy(token = value.trim())
    }
    fun input(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(input = if (_state.value.novelMode) value else value.take(2000)) }
    fun mode(value: ChatMode) {
        if (!_state.value.busy && _state.value.mode != value)
            _state.value = _state.value.copy(mode = value, messages = emptyList(), notice = "已切换模式并开始新对话")
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
            _state.value = _state.value.copy(token = "",
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
    fun saveNovelMemory(value: NovelMemory) {
        if (_state.value.busy) return
        perform {
            withContext(Dispatchers.IO) { settings.saveNovelMemory(value) }
            _state.value = _state.value.copy(novelMemory = value, notice = "小说资料已保存到本机，下次小说请求开始使用")
        }
    }
    fun summarizeChapter() {
        if (_state.value.novelMode && _state.value.messages.any { it.role == "assistant" })
            sendInput(NovelRules.SUMMARY_REQUEST, preserveDraft = true)
    }
    fun send() = sendInput(_state.value.input)

    private fun request(state: ChatUiState, history: List<ChatMessage>, input: String) =
        if (state.novelMode) NovelRules.messages(history, input, state.novelMemory)
        else ChatRules.messages(history, input, state.promptMode, state.promptRules)

    private fun sendInput(input: String, preserveDraft: Boolean = false) {
        val state = _state.value
        if (state.busy) return
        perform(foreground = true) {
            val request = request(state, state.messages, input)
            val activeToken = if (state.provider == ChatProvider.APIPOD) withContext(Dispatchers.IO) { settings.load(ChatProvider.APIPOD).token } else state.token
            if (state.provider == ChatProvider.APIPOD && activeToken.isBlank()) throw java.io.IOException("APIPod Key 尚未保存，请先在设置中保存 Key")
            if (state.provider == ChatProvider.APIPOD && activeToken != state.token) _state.value = _state.value.copy(token = activeToken)
            val answer = client.reply(state.provider, activeToken, state.model, request, state.novelMode)
            _state.value = _state.value.copy(input = if (preserveDraft) state.input else "",
                messages = request.drop(1) + ChatMessage("assistant", answer.text), notice = answer.notice)
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
            val request = request(state, previousMessages, lastUserText)
            val activeToken = if (state.provider == ChatProvider.APIPOD) withContext(Dispatchers.IO) { settings.load(ChatProvider.APIPOD).token } else state.token
            if (state.provider == ChatProvider.APIPOD && activeToken.isBlank()) throw java.io.IOException("APIPod Key 尚未保存，请先在设置中保存 Key")
            if (state.provider == ChatProvider.APIPOD && activeToken != state.token) _state.value = _state.value.copy(token = activeToken)
            val answer = client.reply(state.provider, activeToken, state.model, request, state.novelMode)
            _state.value = _state.value.copy(messages = request.drop(1) + ChatMessage("assistant", answer.text), notice = answer.notice)
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
