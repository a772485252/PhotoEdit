package com.photoedit.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import com.photoedit.app.viewmodel.DressChangeUiState

@Composable
fun DressChangeScreen(
    state: DressChangeUiState,
    onChoose: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("一键变装卡点", style = MaterialTheme.typography.titleLarge)
        Text("本地选择照片，按流行变装节奏自动安排切换时间。", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onChoose, modifier = Modifier.fillMaxWidth()) {
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
                        val thumbnail = remember(uri) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                    BitmapFactory.decodeStream(stream, null, options)
                                    val sample = maxOf(1, maxOf(options.outWidth, options.outHeight) / 256)
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample })
                                    }
                                }
                            } catch (_: Exception) { null }
                        }
                        if (thumbnail != null) Image(thumbnail.asImageBitmap(), "第 ${index + 1} 张", Modifier.size(64.dp), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f)) {
                            Text("照片 ${index + 1}", style = MaterialTheme.typography.titleSmall)
                            state.timeline?.cues?.getOrNull(index)?.let { cue ->
                                Text("${cue.startMs / 1000.0}–${cue.endMs / 1000.0} 秒", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) { Text("↑") }
                        TextButton(onClick = { onMove(index, index + 1) }, enabled = index < state.photos.lastIndex) { Text("↓") }
                        TextButton(onClick = { onRemove(index) }) { Text("删") }
                    }
                }
            }
            item {
                Text("视频编码、运镜和自动配乐仍在开发中。当前页面仅提供照片排序和卡点时间轴预览，不会生成视频。", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
