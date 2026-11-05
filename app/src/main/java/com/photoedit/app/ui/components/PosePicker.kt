package com.photoedit.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.photoedit.app.R
import com.photoedit.app.domain.PoseCatalog
import com.photoedit.app.domain.PosePreset

/** Bundled, independently drawn vectors. Opening or selecting never uses the network. */
@Composable
fun PosePicker(onDismiss: () -> Unit, onSelect: (PosePreset) -> Unit) {
    var category by remember { mutableStateOf("全部") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = PoseCatalog.poses.find { it.id == selectedId }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择姿势", style = MaterialTheme.typography.titleLarge)
                Text("30 个精选姿势 · 原创通用简图 · 离线可用", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("全部", "站姿", "坐姿", "跪姿", "卧姿", "特殊").forEach { item ->
                        FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
                    }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PoseCatalog.poses.filter { category == "全部" || it.category == category }, key = { it.id }) { pose ->
                        Card(onClick = { selectedId = pose.id }, colors = CardDefaults.cardColors(
                            containerColor = if (pose.id == selectedId) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                Image(painterResource(poseDrawable(pose.category)), pose.title,
                                    Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit)
                                Text(if (pose.id == selectedId) "✓ ${pose.id} ${pose.title}" else "${pose.id} ${pose.title}",
                                    style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                selected?.let {
                    Text(it.prompt, modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("通用简图仅示意类别，不是精确骨架；以动作提示词为准。图片不会上传。", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { selected?.let(onSelect) }, enabled = selected != null) { Text("使用姿势") }
                }
            }
        }
    }
}

private fun poseDrawable(category: String): Int = when (category) {
    "站姿" -> R.drawable.pose_hips
    "坐姿" -> R.drawable.pose_sit_relaxed
    "跪姿" -> R.drawable.pose_sit_thighs
    "卧姿" -> R.drawable.pose_sit_open
    "特殊" -> R.drawable.pose_wide
    else -> R.drawable.pose_relaxed
}
