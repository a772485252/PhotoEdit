package com.photoedit.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photoedit.app.ai.AiSettingsStore
import com.photoedit.app.ai.GroqChatClient
import com.photoedit.app.domain.AiRules
import com.photoedit.app.domain.ChatMessage
import com.photoedit.app.domain.ChatRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class ChatUiState(val token: String = "", val input: String = "", val promptMode: Boolean = true,
    val messages: List<ChatMessage> = emptyList(), val busy: Boolean = true, val notice: String? = null)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = AiSettingsStore(application, "groq_chat_private")
    private val client = GroqChatClient()
    private val _state = MutableStateFlow(ChatUiState())
    val uiState = _state.asStateFlow()
    init { perform { val token = withContext(Dispatchers.IO) { settings.load().second }; _state.value = _state.value.copy(token = token) } }
    fun token(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(token = value.trim()) }
    fun input(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(input = value.take(2000)) }
    fun mode(value: Boolean) {
        if (!_state.value.busy && _state.value.promptMode != value) _state.value = _state.value.copy(promptMode = value, messages = emptyList(), notice = "已切换模式并开始新对话")
    }
    fun clearChat() { if (!_state.value.busy) _state.value = _state.value.copy(messages = emptyList(), notice = null) }
    fun saveKey() {
        if (_state.value.busy) return
        val key = _state.value.token
        perform { withContext(Dispatchers.IO) { settings.save(AiRules.DEFAULT_ACCOUNT, key) }; _state.value = _state.value.copy(notice = "Groq Key 已加密保存在本机，尚未验证 API 权限") }
    }
    fun clearKey() {
        if (_state.value.busy) return
        perform { withContext(Dispatchers.IO) { settings.clear() }; _state.value = _state.value.copy(token = "", notice = "已清除 Groq Key；Cloudflare 设置不受影响") }
    }
    fun send() {
        val state = _state.value
        if (state.busy) return
        perform {
            val request = ChatRules.messages(state.messages, state.input, state.promptMode)
            val answer = client.reply(state.token, request)
            _state.value = _state.value.copy(input = "", messages = (request.drop(1) + ChatMessage("assistant", answer)), notice = null)
        }
    }
    private fun perform(block: suspend () -> Unit) {
        _state.value = _state.value.copy(busy = true, notice = null)
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: SocketTimeoutException) { _state.value = _state.value.copy(notice = "Groq 响应超时，未自动重试；输入已保留") }
            catch (_: UnknownHostException) { _state.value = _state.value.copy(notice = "无法连接 Groq，请检查网络；输入已保留") }
            catch (e: Exception) { _state.value = _state.value.copy(notice = if (e is IllegalArgumentException || e is java.io.IOException) e.message else "聊天操作失败，请检查设置后重试") }
            finally { _state.value = _state.value.copy(busy = false) }
        }
    }
}
