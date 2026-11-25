package com.photoedit.app.ui.screens

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.photoedit.app.ui.components.ScreenHeading
import com.photoedit.app.viewmodel.DressChangeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DressChangeScreen(
    state: DressChangeUiState,
    onChoose: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit
) {
    val editingEnabled = !state.exporting && !state.saving
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val actionMaxHeight = maxHeight * 0.4f
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    ScreenHeading("变装视频", "照片按顺序切换，配合原创电子节奏 · 本地制作")
                }
                item {
                    OutlinedButton(onClick = onChoose, enabled = editingEnabled, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.photos.isEmpty()) "选择 2～30 张照片" else "重新选择照片")
                    }
                }
                item {
                    Text(state.timeline?.let { "${state.photos.size} 张照片 · ${it.durationMs / 1000} 秒 · 按下方顺序播放" }
                        ?: "至少选择两张照片，再调整播放顺序。", style = MaterialTheme.typography.bodyMedium)
                    state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
                itemsIndexed(state.photos, key = { _, uri -> uri.toString() }) { index, uri ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val context = LocalContext.current
                                var thumbnail by remember(uri) { mutableStateOf<Bitmap?>(null) }
                                LaunchedEffect(uri) { thumbnail = loadDressThumbnail(context, uri) }
                                val loadedThumbnail = thumbnail
                                if (loadedThumbnail != null) Image(loadedThumbnail.asImageBitmap(), "第 ${index + 1} 张照片",
                                    Modifier.size(64.dp), contentScale = ContentScale.Crop)
                                Column(Modifier.weight(1f)) {
                                    Text("照片 ${index + 1}", style = MaterialTheme.typography.titleSmall)
                                    state.timeline?.cues?.getOrNull(index)?.let { cue ->
                                        Text("${cue.startMs / 1000.0}–${cue.endMs / 1000.0} 秒", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { onMove(index, index - 1) }, enabled = editingEnabled && index > 0) { Text("上移") }
                                TextButton(onClick = { onMove(index, index + 1) }, enabled = editingEnabled && index < state.photos.lastIndex) { Text("下移") }
                                TextButton(onClick = { onRemove(index) }, enabled = editingEnabled) { Text("移除") }
                            }
                        }
                    }
                }
                state.videoPath?.let { previewPath ->
                    item {
                        HorizontalDivider()
                        Text("视频预览", style = MaterialTheme.typography.titleMedium)
                        AndroidView(factory = { context -> VideoView(context).apply {
                            tag = previewPath
                            setVideoPath(previewPath)
                            setOnPreparedListener { player -> player.isLooping = true; start() }
                        } }, update = { view ->
                            if (view.tag != previewPath) {
                                view.tag = previewPath
                                view.setVideoPath(previewPath)
                                view.start()
                            }
                        }, modifier = Modifier.fillMaxWidth().height(240.dp))
                    }
                }
                item {
                    Text("配乐为原创合成节奏。视频先生成到本机缓存，点击保存后才进入相册。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(tonalElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().heightIn(max = actionMaxHeight)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.exporting) {
                        LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("正在离线生成：${state.progress}%", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.videoPath != null) {
                        Button(onClick = onSave, enabled = editingEnabled && state.savedVideoUri == null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (state.saving) "正在保存…" else if (state.savedVideoUri != null) "已保存到相册" else "保存视频到相册")
                        }
                        TextButton(onClick = onExport, enabled = editingEnabled, modifier = Modifier.fillMaxWidth()) { Text("重新生成视频") }
                    } else {
                        Button(onClick = onExport, enabled = state.timeline != null && editingEnabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("生成带节奏视频（测试版）") }
                    }
                }
            }
        }
    }
}

private suspend fun loadDressThumbnail(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / sample > 256) sample *= 2
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        } catch (_: Exception) {
            null
        }
    }
