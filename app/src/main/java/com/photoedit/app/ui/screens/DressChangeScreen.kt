package com.photoedit.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import com.photoedit.app.viewmodel.DressChangeUiState

@Composable
fun DressChangeScreen(
    state: DressChangeUiState,
    onChoose: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("一键变装卡点", style = MaterialTheme.typography.titleLarge)
        Text("本地选择照片，按流行变装节奏自动安排切换时间。", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onChoose, enabled = !state.exporting, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.photos.isEmpty()) "选择 2～30 张照片" else "重新选择照片")
        }
        state.timeline?.let {
            Spacer(Modifier.height(8.dp))
            Text("已选 ${state.photos.size} 张 · ${it.durationMs / 1000} 秒 · ${it.templateId}")
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.photos, key = { _, uri -> uri.toString() }) { index, uri ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val context = LocalContext.current
                        var thumbnail by remember(uri) { mutableStateOf<Bitmap?>(null) }
                        LaunchedEffect(uri) {
                            thumbnail = loadDressThumbnail(context, uri)
                        }
                        val loadedThumbnail = thumbnail
                        if (loadedThumbnail != null) Image(loadedThumbnail.asImageBitmap(), "第 ${index + 1} 张", Modifier.size(64.dp), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f)) {
                            Text("照片 ${index + 1}", style = MaterialTheme.typography.titleSmall)
                            state.timeline?.cues?.getOrNull(index)?.let { cue ->
                                Text("${cue.startMs / 1000.0}–${cue.endMs / 1000.0} 秒", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = { onMove(index, index - 1) }, enabled = !state.exporting && index > 0) { Text("↑") }
                        TextButton(onClick = { onMove(index, index + 1) }, enabled = !state.exporting && index < state.photos.lastIndex) { Text("↓") }
                        TextButton(onClick = { onRemove(index) }, enabled = !state.exporting) { Text("删") }
                    }
                }
            }
            item {
                Text("当前自动合成原创电子节奏，不使用网络热门歌曲。", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.exporting) {
            LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
            Text("正在离线编码：${state.progress}%")
        }
        Button(onClick = onExport, enabled = state.timeline != null && !state.exporting, modifier = Modifier.fillMaxWidth()) {
            Text("生成带节奏 MP4（测试版）")
        }
        if (state.videoPath != null) {
            Text("视频已生成，可预览并主动保存到相册。", style = MaterialTheme.typography.bodySmall)
            val previewPath = state.videoPath
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
            }, modifier = Modifier.fillMaxWidth().height(160.dp))
            Button(onClick = onSave, enabled = !state.exporting && !state.saving && state.savedVideoUri == null, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.saving) "正在保存…" else if (state.savedVideoUri != null) "已保存到相册" else "保存视频到相册")
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
