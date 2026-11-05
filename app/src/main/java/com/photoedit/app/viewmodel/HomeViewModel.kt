package com.photoedit.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photoedit.app.domain.GridSpec
import com.photoedit.app.processing.ImageProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val image: Bitmap? = null,
    val gridSize: Int = 1,
    val tiles: List<Bitmap> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val processor = ImageProcessor(application)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun onPhotoSelected(uri: Uri?) {
        if (uri == null || _uiState.value.busy) return
        perform {
            val image = processor.load(uri)
            _uiState.value = _uiState.value.copy(image = image, tiles = emptyList(), message = "图片已加载")
        }
    }

    fun selectGrid(size: Int) {
        if (_uiState.value.busy) return
        GridSpec(size)
        _uiState.value = _uiState.value.copy(gridSize = size, tiles = emptyList(), message = null)
    }

    fun split() {
        val state = _uiState.value
        val image = state.image ?: return
        if (state.busy) return
        perform {
            val tiles = processor.preview(image, GridSpec(state.gridSize))
            _uiState.value = _uiState.value.copy(tiles = tiles, message = "拆分完成，按从左到右、从上到下的顺序保存")
        }
    }

    fun save() {
        val state = _uiState.value
        val image = state.image ?: return
        if (state.busy || state.tiles.isEmpty()) return
        perform {
            val count = processor.save(image, GridSpec(state.gridSize))
            _uiState.value = _uiState.value.copy(message = "已保存 $count 张 PNG 图片到手机相册（PhotoEdit）")
        }
    }

    fun permissionDenied() {
        _uiState.value = _uiState.value.copy(message = "保存需要存储权限，请允许后重试")
    }

    private fun perform(action: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                _uiState.value = _uiState.value.copy(message = "处理失败：${error.localizedMessage ?: "请重试"}")
            } catch (_: OutOfMemoryError) {
                _uiState.value = _uiState.value.copy(message = "图片过大，内存不足，请换一张较小的图片")
            } finally { _uiState.value = _uiState.value.copy(busy = false) }
        }
    }
}
