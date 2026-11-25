package com.photoedit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.photoedit.app.ui.components.PhotoPreview
import com.photoedit.app.ui.components.ScreenHeading
import com.photoedit.app.viewmodel.HomeUiState

@Composable
fun HomeScreen(state: HomeUiState, onChoosePhoto: () -> Unit, onGrid: (Int) -> Unit,
               onSplit: () -> Unit, onSave: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val actionMaxHeight = maxHeight * 0.4f
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ScreenHeading("图片拆分", "选择图片，均匀拆分，再保存到相册 · 全程离线")
                OutlinedButton(onClick = onChoosePhoto, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.image == null) "从相册选择图片" else "更换图片")
                }
                state.image?.let { image ->
                    PhotoPreview(image, "所选图片预览")
                    Text("处理尺寸：${image.width} × ${image.height}", style = MaterialTheme.typography.labelLarge)
                    Text("大图会适度缩小，按此尺寸导出 PNG。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } ?: Text("还没有选择图片。选图后可查看预览和拆分结果。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("拆分方式", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        (1..2).forEach { size ->
                            FilterChip(selected = state.gridSize == size, onClick = { onGrid(size) },
                                enabled = !state.busy, label = { Text(if (size == 1) "1 × 2" else "2 × 2") })
                        }
                    }
                    Text("保留图片比例，仅裁去不能均分的少量边缘像素。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (state.tiles.isNotEmpty()) {
                    HorizontalDivider()
                    Text("拆分结果 · ${state.tiles.size} 张", style = MaterialTheme.typography.titleMedium)
                    state.tiles.chunked(2).forEachIndexed { row, tiles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tiles.forEachIndexed { col, tile ->
                                val number = row * 2 + col + 1
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PhotoPreview(tile, "第 $number 张拆分结果")
                                    Text("第 $number 张", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
            Surface(tonalElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().heightIn(max = actionMaxHeight)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("正在处理…", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.tiles.isNotEmpty()) {
                        Button(onClick = onSave, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("保存全部 ${state.tiles.size} 张到相册")
                        }
                        TextButton(onClick = onSplit, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("重新拆分") }
                    } else {
                        Button(onClick = onSplit, enabled = state.image != null && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("拆分并预览") }
                    }
                }
            }
        }
    }
}
