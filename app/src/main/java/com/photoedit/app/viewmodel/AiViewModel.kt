package com.photoedit.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photoedit.app.ai.AiSettingsStore
import com.photoedit.app.ai.CloudflareClient
import com.photoedit.app.ai.RunwareClient
import com.photoedit.app.domain.AiRules
import com.photoedit.app.processing.GalleryWriter
import com.photoedit.app.processing.ImageProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class ImageTurn(val prompt: String, val image: Bitmap, val referenceUsed: Boolean)

// Token is in volatile ViewModel memory; never save UI state into a Bundle or logs.
data class AiUiState(
    val account: String = AiRules.DEFAULT_ACCOUNT,
    val token: String = "",
    val runwareKey: String = "",
    val provider: String = "cloudflare",
    val prompt: String = "",
    val reference: Bitmap? = null,
    val result: Bitmap? = null,
    val turns: List<ImageTurn> = emptyList(),
    val continueEditing: Boolean = true,
    val busy: Boolean = true,
    val message: String? = null
)

class AiViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = AiSettingsStore(application)
    private val runwareSettings = AiSettingsStore(application, "runware_private")
    private val runwareClient = RunwareClient()
    private val processor = ImageProcessor(application)
    private val gallery = GalleryWriter(application)
    private val client = CloudflareClient()
    private val _state = MutableStateFlow(AiUiState())
    val uiState = _state.asStateFlow()
    init {
        perform {
            val (account, token) = withContext(Dispatchers.IO) { settings.load() }
            val (_, runwareKey) = withContext(Dispatchers.IO) { runwareSettings.load() }
            _state.value = _state.value.copy(account = account, token = token, runwareKey = runwareKey)
        }
    }
    fun account(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(account = value.trim()) }
    fun token(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(token = value.trim()) }
    fun provider(value: String) { if (!_state.value.busy && value in listOf("cloudflare", "runware")) _state.value = _state.value.copy(provider = value, result = null, turns = emptyList(), reference = null) }
    fun runwareKey(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(runwareKey = value.trim()) }
    fun saveRunwareKey() {
        if (_state.value.busy) return
        val key = _state.value.runwareKey
        perform {
            withContext(Dispatchers.IO) { runwareSettings.save(AiRules.DEFAULT_ACCOUNT, key) }
            _state.value = _state.value.copy(message = "Runware Key 已加密保存")
        }
    }
    fun clearRunwareKey() {
        if (_state.value.busy) return
        perform {
            withContext(Dispatchers.IO) { runwareSettings.clear() }
            _state.value = _state.value.copy(runwareKey = "", message = "已清除 Runware Key")
        }
    }
    fun prompt(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(prompt = value.take(4000)) }
    fun saveSettings() {
        if (_state.value.busy) return
        val state = _state.value
        perform {
            withContext(Dispatchers.IO) { settings.save(state.account, state.token) }
            _state.value = _state.value.copy(message = "设置已加密保存在本机；尚未验证 API 权限")
        }
    }
    fun clearSettings() {
        if (_state.value.busy) return
        perform {
            withContext(Dispatchers.IO) { settings.clear() }
            _state.value = _state.value.copy(account = AiRules.DEFAULT_ACCOUNT, token = "", message = "已清除本机保存的 Token")
        }
    }
    fun selectReference(uri: Uri?) {
        if (uri == null || _state.value.busy) return
        perform {
            val bitmap = processor.load(uri)
            _state.value = _state.value.copy(reference = bitmap, message = "参考图已加载，只有点击生成才上传")
        }
    }
    fun removeReference() { if (!_state.value.busy) _state.value = _state.value.copy(reference = null) }
    fun setContinueEditing(value: Boolean) { if (!_state.value.busy) _state.value = _state.value.copy(continueEditing = value) }
    fun useTurn(index: Int) { if (!_state.value.busy) _state.value.turns.getOrNull(index)?.let { _state.value = _state.value.copy(reference = it.image, continueEditing = false, message = "已选中历史图片作为参考") } }
    fun newSession() { if (!_state.value.busy) _state.value = _state.value.copy(turns = emptyList(), result = null, reference = null, prompt = "", message = null, continueEditing = true) }
    fun generate() {
        if (_state.value.busy) return
        val state = _state.value
        perform {
            val selected = if (state.continueEditing && state.result != null) state.result else state.reference
            val image = if (state.provider == "runware") runwareClient.generate(state.runwareKey, state.prompt.trim(), selected)
                else client.generate(state.account, state.token, state.prompt.trim(), selected)
            _state.value = _state.value.copy(result = image, turns = state.turns + ImageTurn(state.prompt.trim(), image, selected != null), prompt = "", continueEditing = true, message = "生成完成；可继续输入修改要求")
        }
    }
    fun saveImage() {
        if (_state.value.busy) return
        val image = _state.value.result ?: return
        perform {
            gallery.saveImage(image)
            _state.value = _state.value.copy(message = "图片已保存到相册（PhotoEdit）")
        }
    }
    fun permissionDenied() { _state.value = _state.value.copy(message = "保存需要存储权限，请允许后重试") }
    private fun perform(action: suspend () -> Unit) {
        _state.value = _state.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (_: SocketTimeoutException) { _state.value = _state.value.copy(message = "请求超时，结果未知。请检查控制台用量；未自动重试。") }
            catch (_: UnknownHostException) { _state.value = _state.value.copy(message = "无法连接 Cloudflare，请检查网络") }
            catch (error: Exception) {
                val safe = if (error is IllegalArgumentException || error is java.io.IOException) error.message else "操作失败，请清除设置后重试"
                _state.value = _state.value.copy(message = safe ?: "操作失败，请重试")
            } catch (_: OutOfMemoryError) { _state.value = _state.value.copy(message = "内存不足，请使用较小的图片") }
            finally { _state.value = _state.value.copy(busy = false) }
        }
    }
}
