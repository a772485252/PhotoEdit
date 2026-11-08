package com.photoedit.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.photoedit.app.domain.DressChangePlanner
import com.photoedit.app.domain.DressChangeTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DressChangeUiState(
    val photos: List<Uri> = emptyList(),
    val timeline: DressChangeTimeline? = null,
    val message: String? = null
)

class DressChangeViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(DressChangeUiState())
    val uiState = mutableState.asStateFlow()

    fun selectPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val selected = uris.distinct().take(30)
        mutableState.value = DressChangeUiState(
            photos = selected,
            timeline = if (selected.size >= 2) DressChangePlanner.plan(selected.size) else null,
            message = if (selected.size < 2) "至少选择两张照片" else if (uris.size > 30) "最多使用 30 张照片" else null
        )
    }

    fun move(from: Int, to: Int) {
        val current = mutableState.value
        if (from !in current.photos.indices || to !in current.photos.indices || from == to) return
        val photos = current.photos.toMutableList().apply { add(to, removeAt(from)) }
        mutableState.value = current.copy(photos = photos)
    }

    fun remove(index: Int) {
        val current = mutableState.value
        if (index !in current.photos.indices) return
        val photos = current.photos.toMutableList().apply { removeAt(index) }
        mutableState.value = current.copy(
            photos = photos,
            timeline = if (photos.size >= 2) DressChangePlanner.plan(photos.size) else null,
            message = if (photos.size < 2) "至少选择两张照片" else null
        )
    }
}
