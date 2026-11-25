package com.photoedit.app.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.photoedit.app.domain.ApiPodRules
import com.photoedit.app.ui.components.PhotoPreview
import com.photoedit.app.viewmodel.AiUiState

@Composable
fun AiScreen(state: AiUiState, onPrompt: (String) -> Unit,
             onChoose: () -> Unit, onRemove: () -> Unit, onGenerate: () -> Unit, onSaveImage: () -> Unit,
 onNewSession: () -> Unit, onUseTurn: (Int) -> Unit, onContinue: (Boolean) -> Unit,
 onApiPodKey: (String) -> Unit, onSaveApiPodKey: () -> Unit, onClearApiPodKey: () -> Unit,
 onApiPodPro: (Boolean) -> Unit, onStopApiPodWaiting: () -> Unit, onAbandonApiPodTracking: () -> Unit) {
    val context = LocalContext.current
    val history = rememberLazyListState()
    var showSettings by remember { mutableStateOf(false) }
    var showReference by remember { mutableStateOf(false) }
    var enlargedResult by remember { mutableStateOf<Bitmap?>(null) }
    val pendingTask = state.apiPodPending
    val effectiveReference = (state.continueEditing && state.result != null) || state.reference != null
    val apiPodModel = ApiPodRules.model(effectiveReference, state.apiPodPro)
    val apiPodEstimate = ApiPodRules.estimate(effectiveReference, state.apiPodPro)
    var confirmAbandon by remember { mutableStateOf(false) }
    val editingEnabled = !state.busy && !pendingTask
    val hasKey = state.apiPodKey.isNotBlank()

    LaunchedEffect(state.turns.size) {
        // One footer item always exists, including in an empty conversation.
        history.animateScrollToItem(state.turns.size)
    }
    if (showReference && state.reference != null) {
        Dialog(onDismissRequest = { showReference = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(12.dp)) {
                    PhotoPreview(state.reference, "参考图大图")
                    TextButton(onClick = { showReference = false }) { Text("关闭") }
                }
            }
        }
    }
    enlargedResult?.let { image ->
        ZoomableResultDialog(image, onDismiss = { enlargedResult = null })
    }
    if (confirmAbandon) {
        AlertDialog(onDismissRequest = { confirmAbandon = false }, title = { Text("结束本地跟踪？") },
            text = { Text("这不会取消 APIPod 平台任务，也不会退费。平台任务可能仍在运行，请先在控制台核对，避免再次提交重复任务。") },
            confirmButton = { TextButton(onClick = { confirmAbandon = false; onAbandonApiPodTracking() }) { Text("结束跟踪") } },
            dismissButton = { TextButton(onClick = { confirmAbandon = false }) { Text("保留任务") } })
    }
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("图片生成设置", style = MaterialTheme.typography.titleLarge)
                    Text("APIPod", style = MaterialTheme.typography.titleSmall)
                    Text("按次付费，不是免费额度。文生图约 $0.015/次，Pro 约 $0.025/次，参考图编辑约 $0.04/次；实际以平台价格与结算为准。", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(state.apiPodKey, onApiPodKey, label = { Text("APIPod API Key") }, singleLine = true,
                        enabled = editingEnabled, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                    Row {
                        TextButton(onClick = onSaveApiPodKey, enabled = editingEnabled) { Text("保存 Key") }
                        TextButton(onClick = onClearApiPodKey, enabled = editingEnabled) { Text("清除 Key") }
                    }
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.apipod.ai/"))) }) { Text("打开 APIPod 控制台") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(state.apiPodPro, onApiPodPro, enabled = editingEnabled && !effectiveReference)
                        Text("文生图使用 Pro", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("本次模型：$apiPodModel", style = MaterialTheme.typography.bodySmall)
                    Text("无参考图使用 Z-Image；有参考图或继续编辑时使用 Qwen Image Edit。上传图片最长边 1024 像素，编辑输出跟随主图；文生图尺寸由模型决定。", style = MaterialTheme.typography.bodySmall)
                    Text("Spicy 路线面向成年人，平台明确不向中国大陆、香港、澳门开放。VPN 不保证账号或模型可用；请先确认地区资格。平台与上游内容规则仍生效。", style = MaterialTheme.typography.bodySmall)
                    Text("点击生成才会向 APIPod 发送提示词和参考图。不会自动重试。人物一致性需以结果为准。", style = MaterialTheme.typography.bodySmall)
                    state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { showSettings = false }, modifier = Modifier.align(Alignment.End)) { Text("完成") }
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val composerMaxHeight = maxHeight * 0.55f
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showSettings = true }, modifier = Modifier.weight(1f)) {
                    Text("APIPod · 图片设置 ▾", maxLines = 1, style = MaterialTheme.typography.titleSmall)
                }
                TextButton(onClick = onNewSession, enabled = editingEnabled) { Text("新会话") }
            }
            HorizontalDivider()
            LazyColumn(state = history, modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(state.turns) { index, turn ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(Modifier.align(Alignment.End).widthIn(max = 320.dp),
                            shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            SelectionContainer { Text(turn.prompt, Modifier.padding(12.dp)) }
                        }
                        PhotoPreview(turn.image, "第 ${index + 1} 次生成结果", Modifier.clickable { enlargedResult = turn.image })
                        Text("点击图片可全屏查看、双指缩放", style = MaterialTheme.typography.labelSmall)
                        turn.costUsd?.let { Text("平台结算：$" + it.toString(), style = MaterialTheme.typography.labelSmall) }
                        Row {
                            TextButton(onClick = { onUseTurn(index) }, enabled = editingEnabled) { Text("继续编辑") }
                            if (index == state.turns.lastIndex) {
                                TextButton(onClick = onSaveImage, enabled = !state.busy) { Text("保存图片") }
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.turns.isEmpty() && !state.busy) {
                            Text("想创作什么画面？", style = MaterialTheme.typography.titleLarge)
                            Text("输入描述，或点 + 添加参考图。", style = MaterialTheme.typography.bodyMedium)
                            if (!hasKey) TextButton(onClick = { showSettings = true }) { Text("设置 API Key") }
                        }
                        if (state.apiPodPending) {
                            Text("原操作已保留，继续查询不会另建新的逻辑任务。", style = MaterialTheme.typography.bodySmall)
                            if (state.busy) TextButton(onClick = onStopApiPodWaiting) { Text("停止本机等待") }
                            else TextButton(onClick = { confirmAbandon = true }) { Text("结束本地跟踪…") }
                        }
                        if (state.busy) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("正在处理…熄屏后会尝试继续；系统允许通知时会显示状态。", style = MaterialTheme.typography.bodySmall)
                        }
                        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            // Bound the composer even on short screens/large fonts, so every action remains scrollable.
            Surface(tonalElevation = 2.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                Column(Modifier.fillMaxWidth().heightIn(max = composerMaxHeight)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.reference?.let { reference ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(reference.asImageBitmap(), "查看参考图", Modifier.size(52.dp).clickable { showReference = true },
                                contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text("已添加参考图", style = MaterialTheme.typography.labelMedium)
                                TextButton(onClick = onChoose, enabled = editingEnabled, contentPadding = PaddingValues(0.dp)) { Text("更换") }
                            }
                            TextButton(onClick = onRemove, enabled = editingEnabled) { Text("移除") }
                        }
                    }
                    if (state.result != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.continueEditing, onCheckedChange = onContinue, enabled = editingEnabled)
                            Text("继续编辑上一张结果", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(when {
                        pendingTask -> "原任务已保留，请继续查询或处理原任务。"
                        state.continueEditing && state.result != null -> "本次参考：上一张生成结果"
                        state.reference != null -> "本次参考：已选图片"
                        else -> "本次参考：无（文生图）"
                    }, style = MaterialTheme.typography.labelSmall)
                    Text(apiPodModel + " · 约 $" + apiPodEstimate + "/次 · 以平台结算为准", style = MaterialTheme.typography.labelSmall)
                    if (!hasKey) TextButton(onClick = { showSettings = true }) { Text("先设置 APIPod Key") }
                    OutlinedTextField(state.prompt, onPrompt, placeholder = { Text("描述画面或修改要求…") },
                        minLines = 1, maxLines = 4, enabled = editingEnabled,
                        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                    if (state.reference == null) {
                        OutlinedButton(onClick = onChoose, enabled = editingEnabled, modifier = Modifier.fillMaxWidth()) { Text("添加参考图") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy && hasKey && (pendingTask || state.prompt.isNotBlank()),
                            shape = RoundedCornerShape(24.dp)) { Text(if (state.busy) "处理中" else if (pendingTask) "继续查询" else "生成 · $" + apiPodEstimate) }
                    }
                }
            }
        }
    }
}

/** Full-screen local preview; does not upload or re-encode the original bitmap. */
@Composable
private fun ZoomableResultDialog(image: Bitmap, onDismiss: () -> Unit) {
    var scale by remember(image) { mutableFloatStateOf(1f) }
    var offset by remember(image) { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "生成图片全屏预览",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
                    .pointerInput(image) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val next = (scale * zoom).coerceIn(1f, 5f)
                            scale = next
                            offset = if (next <= 1f) Offset.Zero else {
                                val maxX = size.width * (next - 1f) / 2f
                                val maxY = size.height * (next - 1f) / 2f
                                Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            }
                        }
                    }
                    .pointerInput(image) {
                        detectTapGestures(onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2f
                            offset = Offset.Zero
                        })
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) { Text("关闭", color = Color.White) }
            Text(
                "双指缩放 · 拖动查看 · 双击切换大小",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
            )
        }
    }
}
