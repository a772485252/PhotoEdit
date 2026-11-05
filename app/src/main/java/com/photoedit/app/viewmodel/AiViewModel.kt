package com.photoedit.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photoedit.app.ai.AiSettingsStore
import com.photoedit.app.ai.CloudflareClient
import com.photoedit.app.ai.HordeClient
import com.photoedit.app.ai.HordeTaskException
import com.photoedit.app.domain.HordeModel
import com.photoedit.app.domain.HordeRules
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean
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
    val provider: String = "horde",
    val hordeKey: String = "",
    val hordeModel: String = "stable_diffusion",
    val hordeNsfw: Boolean = true,
    val hordeModels: List<HordeModel> = emptyList(),
    val hordeTaskPending: Boolean = false,
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
    private val hordeClient = HordeClient()
    private val hordeSettings = AiSettingsStore(application, "horde_private")
    private val imagePreferences = application.getSharedPreferences("image_preferences", android.content.Context.MODE_PRIVATE)
    private data class PendingHorde(val id: String, val prompt: String, val referenceUsed: Boolean)
    private var pendingHorde: PendingHorde? = null
    private var operation: Job? = null
    private val submitUnknown = AtomicBoolean(false)
    private val processor = ImageProcessor(application)
    private val gallery = GalleryWriter(application)
    private val client = CloudflareClient()
    private val _state = MutableStateFlow(AiUiState())
    val uiState = _state.asStateFlow()
    init {
        perform {
            val (account, token) = withContext(Dispatchers.IO) { settings.load() }
            val (_, runwareKey) = withContext(Dispatchers.IO) { runwareSettings.load() }
            val (_, hordeKey) = withContext(Dispatchers.IO) { hordeSettings.load() }
            val provider = imagePreferences.getString("provider", "horde").orEmpty()
                .takeIf { it in listOf("cloudflare", "runware", "horde") } ?: "horde"
            _state.value = _state.value.copy(account = account, token = token, runwareKey = runwareKey,
                hordeKey = hordeKey, provider = provider,
                hordeModel = imagePreferences.getString("horde_model", "stable_diffusion") ?: "stable_diffusion",
                hordeNsfw = imagePreferences.getBoolean("horde_nsfw", true))
        }
    }
    fun account(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(account = value.trim()) }
    fun token(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(token = value.trim()) }
    fun provider(value: String) {
        if (_state.value.busy || value !in listOf("cloudflare", "runware", "horde") || value == _state.value.provider) return
        if (pendingHorde != null) { _state.value = _state.value.copy(message = "请先继续查询或取消 AI Horde 原任务"); return }
        // Keep draft/reference when changing transport; only the provider's result history resets.
        _state.value = _state.value.copy(provider = value, result = null, turns = emptyList(), continueEditing = false)
        imagePreferences.edit().putString("provider", value).apply()
    }
    fun hordeKey(value: String) { if (!_state.value.busy) _state.value = _state.value.copy(hordeKey = value.trim()) }
    fun hordeModel(value: String) {
        if (!_state.value.busy && pendingHorde == null) {
            _state.value = _state.value.copy(hordeModel = value.take(200))
            imagePreferences.edit().putString("horde_model", value.take(200)).apply()
        }
    }
    fun hordeNsfw(value: Boolean) {
        if (!_state.value.busy && pendingHorde == null) {
            _state.value = _state.value.copy(hordeNsfw = value)
            imagePreferences.edit().putBoolean("horde_nsfw", value).apply()
        }
    }
    fun saveHordeKey() {
        if (_state.value.busy) return
        val key = _state.value.hordeKey
        perform {
            HordeRules.apiKey(key)
            withContext(Dispatchers.IO) {
                if (key.isBlank()) hordeSettings.clear() else hordeSettings.save(AiRules.DEFAULT_ACCOUNT, key)
            }
            _state.value = _state.value.copy(message = if (key.isBlank()) "AI Horde 使用匿名免费模式" else "AI Horde Key 已加密保存")
        }
    }
    fun clearHordeKey() {
        if (_state.value.busy) return
        perform {
            withContext(Dispatchers.IO) { hordeSettings.clear() }
            _state.value = _state.value.copy(hordeKey = "", message = "已清除 AI Horde Key，使用匿名模式")
        }
    }
    fun refreshHordeModels() {
        if (_state.value.busy) return
        perform {
            val models = hordeClient.models()
            _state.value = _state.value.copy(hordeModels = models, message = "已读取 ${models.size} 个在线模型；参考图支持取决于节点")
        }
    }
    fun stopHordeWaiting() {
        if (pendingHorde == null || !_state.value.busy) return
        operation?.cancel()
        _state.value = _state.value.copy(message = "已停止本机等待；平台任务可能继续，稍后可继续查询或取消任务")
    }
    fun cancelHordeTask() {
        if (_state.value.busy) return
        val task = pendingHorde ?: return
        perform {
            try { hordeClient.cancel(task.id) }
            catch (error: HordeTaskException) { if (!error.terminal) throw error }
            pendingHorde = null
            _state.value = _state.value.copy(hordeTaskPending = false, message = "原任务未完成部分已取消或任务已过期；草稿已保留")
        }
    }
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
    fun prompt(value: String) { if (!_state.value.busy && pendingHorde == null) _state.value = _state.value.copy(prompt = value.take(4000)) }
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
        if (uri == null || _state.value.busy || pendingHorde != null) return
        perform {
            val bitmap = processor.load(uri)
            _state.value = _state.value.copy(reference = bitmap, continueEditing = false, message = "参考图已加载，只有点击生成才上传")
        }
    }
    fun removeReference() { if (!_state.value.busy && pendingHorde == null) _state.value = _state.value.copy(reference = null) }
    fun setContinueEditing(value: Boolean) { if (!_state.value.busy && pendingHorde == null) _state.value = _state.value.copy(continueEditing = value) }
    fun useTurn(index: Int) { if (!_state.value.busy && pendingHorde == null) _state.value.turns.getOrNull(index)?.let { _state.value = _state.value.copy(reference = it.image, continueEditing = false, message = "已选中历史图片作为参考") } }
    fun newSession() { if (!_state.value.busy && pendingHorde == null) _state.value = _state.value.copy(turns = emptyList(), result = null, reference = null, prompt = "", message = null, continueEditing = true) }
    fun generate() {
        if (_state.value.busy) return
        val state = _state.value
        perform {
            val selected = if (state.continueEditing && state.result != null) state.result else state.reference
            var prompt = state.prompt.trim()
            var referenceUsed = selected != null
            val image = when (state.provider) {
                "horde" -> {
                    var task = pendingHorde
                    if (task == null) {
                        AiRules.validatePrompt(prompt)
                        HordeRules.apiKey(state.hordeKey)
                        HordeRules.validateModel(state.hordeModel)
                        _state.value = _state.value.copy(message = "正在提交 AI Horde，请勿重复操作…")
                        submitUnknown.set(true)
                        val id = try { hordeClient.submit(state.hordeKey, prompt, state.hordeModel, state.hordeNsfw, selected) }
                        catch (error: HordeTaskException) { submitUnknown.set(false); throw error }
                        task = PendingHorde(id, prompt, referenceUsed)
                        pendingHorde = task
                        submitUnknown.set(false)
                        _state.value = _state.value.copy(hordeTaskPending = true)
                    }
                    prompt = task.prompt
                    referenceUsed = task.referenceUsed
                    try {
                        hordeClient.await(task.id) { message ->
                            _state.value = _state.value.copy(message = message)
                        }
                    } catch (error: HordeTaskException) {
                        if (error.terminal) {
                            pendingHorde = null
                            _state.value = _state.value.copy(hordeTaskPending = false)
                        }
                        throw error
                    }
                }
                "runware" -> runwareClient.generate(state.runwareKey, prompt, selected)
                else -> client.generate(state.account, state.token, prompt, selected)
            }
            pendingHorde = null
            _state.value = _state.value.copy(result = image, turns = state.turns + ImageTurn(prompt, image, referenceUsed),
                prompt = "", continueEditing = true, hordeTaskPending = false, message = "生成完成；可继续输入修改要求")
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
        operation = viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (_: SocketTimeoutException) { _state.value = _state.value.copy(message = "请求超时，结果未知。请检查控制台用量；未自动重试。") }
            catch (_: UnknownHostException) { _state.value = _state.value.copy(message = "无法连接所选平台，请检查网络；已有 AI Horde 任务可继续查询") }
            catch (error: Exception) {
                val safe = if (error is IllegalArgumentException || error is java.io.IOException) error.message else "操作失败，请清除设置后重试"
                _state.value = _state.value.copy(message = safe ?: "操作失败，请重试")
            } catch (_: OutOfMemoryError) { _state.value = _state.value.copy(message = "内存不足，请使用较小的图片") }
            finally {
                if (submitUnknown.getAndSet(false)) {
                    _state.value = _state.value.copy(message = "AI Horde 提交未取得任务 ID，是否受理未知；请先在平台核对，勿反复提交。")
                }
                _state.value = _state.value.copy(busy = false)
            }
        }
    }
}
