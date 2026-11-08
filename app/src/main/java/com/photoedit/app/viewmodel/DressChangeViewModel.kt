package com.photoedit.app.viewmodel

import android.net.Uri
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photoedit.app.processing.DressChangeVideoExporter
import com.photoedit.app.processing.DressChangeVideoSaver
import java.io.File
import kotlinx.coroutines.launch
import com.photoedit.app.domain.DressChangePlanner
import com.photoedit.app.domain.DressChangeTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DressChangeUiState(
    val photos: List<Uri> = emptyList(),
    val timeline: DressChangeTimeline? = null,
    val message: String? = null,
    val exporting: Boolean = false,
    val progress: Int = 0,
    val videoPath: String? = null,
    val saving: Boolean = false,
    val savedVideoUri: Uri? = null
)

class DressChangeViewModel(application: Application) : AndroidViewModel(application) {
    private val exporter = DressChangeVideoExporter(application)
    private val saver = DressChangeVideoSaver(application)
    private val mutableState = MutableStateFlow(DressChangeUiState())
    val uiState = mutableState.asStateFlow()

    fun selectPhotos(uris: List<Uri>) {
        if (uris.isEmpty() || mutableState.value.exporting || mutableState.value.saving) return
        val selected = uris.distinct().take(30)
        mutableState.value = DressChangeUiState(
            photos = selected,
            timeline = if (selected.size >= 2) DressChangePlanner.plan(selected.size) else null,
            message = if (selected.size < 2) "至少选择两张照片" else if (uris.size > 30) "最多使用 30 张照片" else null
        )
    }

    fun move(from: Int, to: Int) {
        val current = mutableState.value
        if (current.exporting || current.saving || from !in current.photos.indices || to !in current.photos.indices || from == to) return
        val photos = current.photos.toMutableList().apply { add(to, removeAt(from)) }
        mutableState.value = current.copy(photos = photos, videoPath = null, savedVideoUri = null, message = null)
    }

    fun export() {
        val current = mutableState.value
        val timeline = current.timeline ?: return
        if (current.exporting || current.saving) return
        mutableState.value = current.copy(exporting = true, progress = 0, videoPath = null, savedVideoUri = null, message = null)
        viewModelScope.launch {
            try {
                val output = exporter.export(current.photos, timeline) { percent ->
                    mutableState.value = mutableState.value.copy(progress = percent)
                }
                mutableState.value = mutableState.value.copy(videoPath = output.absolutePath, message = "无声 MP4 已生成到应用缓存，尚未保存到相册")
            } catch (e: Exception) {
                mutableState.value = mutableState.value.copy(message = "视频生成失败：${e.localizedMessage ?: "请重试"}")
            } finally {
                mutableState.value = mutableState.value.copy(exporting = false)
            }
        }
    }

    fun saveVideo() {
        val state = mutableState.value
        val path = state.videoPath ?: return
        if (state.exporting || state.saving) return
        mutableState.value = state.copy(saving = true, message = null)
        viewModelScope.launch {
            try {
                val uri = saver.save(File(path))
                mutableState.value = mutableState.value.copy(savedVideoUri = uri, message = "视频已保存到手机相册")
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(message = "保存失败：${error.localizedMessage ?: "请重试"}")
            } finally {
                mutableState.value = mutableState.value.copy(saving = false)
            }
        }
    }

    fun remove(index: Int) {
        val current = mutableState.value
        if (current.exporting || current.saving || index !in current.photos.indices) return
        val photos = current.photos.toMutableList().apply { removeAt(index) }
        mutableState.value = current.copy(
            photos = photos,
            videoPath = null,
            savedVideoUri = null,
            timeline = if (photos.size >= 2) DressChangePlanner.plan(photos.size) else null,
            message = if (photos.size < 2) "至少选择两张照片" else null
        )
    }
}
