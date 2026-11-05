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
                Text("24 个正面姿势 · 双腿不交叉 · 离线可用", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("全部", "站姿", "坐姿").forEach { item ->
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
                                Image(painterResource(poseDrawable(pose.id)), pose.title,
                                    Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit)
                                Text(if (pose.id == selectedId) "✓ ${pose.title}" else pose.title,
                                    style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                selected?.let {
                    Text(it.prompt, modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("仅填入动作提示词；示意图不会上传。", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { selected?.let(onSelect) }, enabled = selected != null) { Text("使用姿势") }
                }
            }
        }
    }
}

private fun poseDrawable(id: String): Int = when (id) {
    "relaxed" -> R.drawable.pose_relaxed
    "hips" -> R.drawable.pose_hips
    "one_hip" -> R.drawable.pose_one_hip
    "open_arms" -> R.drawable.pose_open_arms
    "low_open" -> R.drawable.pose_low_open
    "high_v" -> R.drawable.pose_high_v
    "overhead" -> R.drawable.pose_overhead
    "wave" -> R.drawable.pose_wave
    "both_wave" -> R.drawable.pose_both_wave
    "shoulders" -> R.drawable.pose_shoulders
    "head_sides" -> R.drawable.pose_head_sides
    "hair" -> R.drawable.pose_hair
    "palms_up" -> R.drawable.pose_palms_up
    "present" -> R.drawable.pose_present
    "waist_front" -> R.drawable.pose_waist_front
    "wide" -> R.drawable.pose_wide
    "bend" -> R.drawable.pose_bend
    "reach" -> R.drawable.pose_reach
    "sit_relaxed" -> R.drawable.pose_sit_relaxed
    "sit_thighs" -> R.drawable.pose_sit_thighs
    "sit_hips" -> R.drawable.pose_sit_hips
    "sit_wave" -> R.drawable.pose_sit_wave
    "sit_open" -> R.drawable.pose_sit_open
    "sit_hair" -> R.drawable.pose_sit_hair
    else -> R.drawable.pose_relaxed
}
