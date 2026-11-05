package com.photoedit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.photoedit.app.ui.components.PhotoPreview
import com.photoedit.app.viewmodel.HomeUiState

@Composable
fun HomeScreen(state: HomeUiState, onChoosePhoto: () -> Unit, onGrid: (Int) -> Unit,
               onSplit: () -> Unit, onSave: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PhotoEdit · 图片拆分", style = MaterialTheme.typography.headlineMedium)
            Text("本地离线处理，图片无需上传")
            Button(onClick = onChoosePhoto, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.image == null) "从相册选择图片" else "重新选择图片")
            }
            state.image?.let { image ->
                PhotoPreview(image, "所选图片预览")
                Text("处理分辨率：${image.width} × ${image.height}")
                Text("大图将适度缩小以控制内存；保存采用此分辨率，PNG 无损输出。", style = MaterialTheme.typography.bodySmall)
            } ?: Text("请选择一张图片开始")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                (1..2).forEach { size ->
                    FilterChip(selected = state.gridSize == size, onClick = { onGrid(size) },
                        enabled = !state.busy, label = { Text(if (size == 1) "1 × 2" else "2 × 2") })
                }
            }
            Text("保持原图比例，按行列均匀拆分；边缘仅裁去无法整除的少量像素。", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onSplit, enabled = state.image != null && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("拆分并预览") }
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("正在处理，请稍候…")
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (state.tiles.isNotEmpty()) {
                Text("拆分结果 · ${state.tiles.size} 张", style = MaterialTheme.typography.titleLarge)
                state.tiles.chunked(2).forEachIndexed { row, tiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tiles.forEachIndexed { col, tile ->
                            Column(Modifier.weight(1f)) {
                                PhotoPreview(tile, "第 ${row * 2 + col + 1} 张拆分结果")
                                Text("${row * state.gridSize + col + 1}")
                            }
                        }
                    }
                }
                Button(onClick = onSave, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("全部保存到手机相册") }
            }
        }
    }
}
