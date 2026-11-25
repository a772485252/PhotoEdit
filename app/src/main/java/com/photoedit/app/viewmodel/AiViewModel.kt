package com.photoedit.app.viewmodel

import android.app.Application
import com.photoedit.app.processing.GenerationForegroundService
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photoedit.app.ai.ApiPodClient
import com.photoedit.app.ai.ApiPodOperation
import com.photoedit.app.ai.ApiPodException
import com.photoedit.app.ai.AiSettingsStore
import kotlinx.coroutines.Job
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

data class ImageTurn(val prompt: String, val image: Bitmap, val referenceUsed: Boolean, val costUsd: Double? = null)

// Token is in volatile ViewModel memory; never save UI state into a Bundle or logs.
data class AiUiState(
    val apiPodKey: String = "",
    val apiPodPro: Boolean = false,
    val apiPodPending: Boolean = false,
    val prompt: String = "",
    val reference: Bitmap? = null,
    val result: Bitmap? = null,
    val turns: List<ImageTurn> = emptyList(),
    val continueEditing: Boolean = true,
    val busy: Boolean = true,
    val message: String? = null
)

class AiViewModel(application: Application) : AndroidViewModel(application) {
    private val apiPodClient = ApiPodClient()
    private val apiPodSettings = AiSettingsStore(application, "apipod_private")
    private var pendingApiPod: ApiPodOperation? = null
    private fun hasPending() = pendingApiPod != null
    private val imagePreferences = application.getSharedPreferences("image_preferences", android.content.Context.MODE_PRIVATE)
    private var operation: Job? = null
    private val processor = ImageProcessor(application)
    private val gallery = GalleryWriter(application)
    private val _state = MutableStateFlow(AiUiState())
    val uiState = _state.asStateFlow()
    init {
        perform {
            val (_, apiPodKey) = withContext(Dispatchers.IO) { apiPodSettings.load() }
            _state.value = _state.value.copy(apiPodKey = apiPodKey,
                apiPodPro = imagePreferences.getBoolean("apipod_pro", false))
        }
    }
    fun apiPodKey(value: String) { if (!_state.value.busy && !hasPending()) _state.value = _state.value.copy(apiPodKey = value.trim()) }
    fun apiPodPro(value: Boolean) {
        if (!_state.value.busy && !hasPending()) {
            _state.value = _state.value.copy(apiPodPro = value)
            imagePreferences.edit().putBoolean("apipod_pro", value).apply()
        }
    }
    fun saveApiPodKey() {
        if (_state.value.busy || hasPending()) return
        val key = _state.value.apiPodKey
        perform {
            com.photoedit.app.domain.ApiPodRules.validateKey(key)
            withContext(Dispatchers.IO) { apiPodSettings.save(AiRules.DEFAULT_ACCOUNT, key) }
            _state.value = _state.value.copy(message = "APIPod Key 已加密保存；尚未验证余额和地区资格")
        }
    }
    fun clearApiPodKey() {
        if (_state.value.busy || hasPending()) return
        perform {
            withContext(Dispatchers.IO) { apiPodSettings.clear() }
            _state.value = _state.value.copy(apiPodKey = "", message = "已清除 APIPod Key")
        }
    }
    fun stopApiPodWaiting() {
        if (pendingApiPod == null || !_state.value.busy) return
        operation?.cancel()
        _state.value = _state.value.copy(message = "已请求停止本机等待；平台任务可能继续并计费，可继续查询原操作")
    }
    fun abandonApiPodTracking() {
        if (_state.value.busy) return
        pendingApiPod = null
        _state.value = _state.value.copy(apiPodPending = false, message = "已结束本机跟踪；没有取消平台任务，可能继续计费，请先在平台核对结果")
    }
    fun prompt(value: String) { if (!_state.value.busy && !hasPending()) _state.value = _state.value.copy(prompt = value.take(4000)) }
    fun selectReference(uri: Uri?) {
        if (uri == null || _state.value.busy || hasPending()) return
        perform {
            val bitmap = processor.load(uri)
            _state.value = _state.value.copy(reference = bitmap, continueEditing = false, message = "参考图已加载，只有点击生成才上传")
        }
    }
    fun removeReference() { if (!_state.value.busy && !hasPending()) _state.value = _state.value.copy(reference = null) }
    fun setContinueEditing(value: Boolean) { if (!_state.value.busy && !hasPending()) _state.value = _state.value.copy(continueEditing = value) }
    fun useTurn(index: Int) { if (!_state.value.busy && !hasPending()) _state.value.turns.getOrNull(index)?.let { _state.value = _state.value.copy(reference = it.image, continueEditing = false, message = "已选中历史图片作为参考") } }
    fun newSession() { if (!_state.value.busy && !hasPending()) _state.value = _state.value.copy(turns = emptyList(), result = null, reference = null, prompt = "", message = null, continueEditing = true) }
    fun generate() {
        if (_state.value.busy) return
        val state = _state.value
        perform(foreground = true) {
            val selected = if (state.continueEditing && state.result != null) state.result else state.reference
            val task = pendingApiPod ?: apiPodClient.prepare(state.apiPodKey, state.prompt.trim(), selected, state.apiPodPro).also {
                pendingApiPod = it
                _state.value = _state.value.copy(apiPodPending = true)
            }
            val result = try {
                apiPodClient.run(task) { message -> _state.value = _state.value.copy(message = message) }
            } catch (error: ApiPodException) {
                if (error.definitive || error.terminal) {
                    pendingApiPod = null
                    _state.value = _state.value.copy(apiPodPending = false)
                }
                throw error
            }
            pendingApiPod = null
            _state.value = _state.value.copy(result = result.image,
                turns = state.turns + ImageTurn(task.prompt, result.image, task.referenceUsed, result.costUsd),
                prompt = "", continueEditing = true, apiPodPending = false, message = "生成完成；可继续输入修改要求")
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
    private fun perform(foreground: Boolean = false, action: suspend () -> Unit) {
        if (foreground) {
            try { GenerationForegroundService.start(getApplication()) }
            catch (_: Exception) {
                _state.value = _state.value.copy(busy = false, message = "无法启动后台生成服务，请保持应用在前台后重试")
                return
            }
        }
        _state.value = _state.value.copy(busy = true, message = null)
        operation = viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (_: SocketTimeoutException) { _state.value = _state.value.copy(message = "请求超时，结果未知。请检查控制台用量；未自动重试。") }
            catch (_: UnknownHostException) { _state.value = _state.value.copy(message = "无法连接所选平台，请检查网络；已有平台任务可继续查询") }
            catch (error: Exception) {
                val safe = if (error is IllegalArgumentException || error is java.io.IOException) error.message else "操作失败，请清除设置后重试"
                _state.value = _state.value.copy(message = safe ?: "操作失败，请重试")
            } catch (_: OutOfMemoryError) { _state.value = _state.value.copy(message = "内存不足，请使用较小的图片") }
            finally {
                _state.value = _state.value.copy(busy = false)
                if (foreground) GenerationForegroundService.stop(getApplication())
            }
        }
    }
}
