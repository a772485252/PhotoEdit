package com.photoedit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.photoedit.app.domain.PosePrompts
import com.photoedit.app.ui.components.PhotoPreview
import com.photoedit.app.ui.components.PosePicker
import com.photoedit.app.viewmodel.AiUiState

@Composable
fun AiScreen(state: AiUiState, onAccount: (String) -> Unit, onToken: (String) -> Unit,
             onPrompt: (String) -> Unit, onSaveSettings: () -> Unit, onClearSettings: () -> Unit,
             onChoose: () -> Unit, onRemove: () -> Unit, onGenerate: () -> Unit, onSaveImage: () -> Unit,
 onNewSession: () -> Unit, onUseTurn: (Int) -> Unit, onContinue: (Boolean) -> Unit,
 onProvider: (String) -> Unit, onRunwareKey: (String) -> Unit, onSaveRunwareKey: () -> Unit, onClearRunwareKey: () -> Unit) {
    val context = LocalContext.current
    val history = rememberLazyListState()
    var showSettings by remember { mutableStateOf(false) }
    var showPoses by remember { mutableStateOf(false) }
    var showAttachment by remember { mutableStateOf(false) }
    var showReference by remember { mutableStateOf(false) }
    val runware = state.provider == "runware"
    val providerName = if (runware) "Runware · Muse Image" else "Cloudflare · FLUX.2 Klein"
    val hasKey = if (runware) state.runwareKey.isNotBlank() else state.token.isNotBlank()

    LaunchedEffect(state.turns.size, state.busy, state.message) {
        // One footer item always exists, including in an empty conversation.
        history.animateScrollToItem(state.turns.size)
    }
    if (showPoses && !state.busy) {
        PosePicker(onDismiss = { showPoses = false }, onSelect = {
            onPrompt(PosePrompts.apply(state.prompt, it))
            showPoses = false
        })
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
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("图片生成设置", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!runware, { onProvider("cloudflare") }, label = { Text("Cloudflare") }, enabled = !state.busy)
                        FilterChip(runware, { onProvider("runware") }, label = { Text("Runware") }, enabled = !state.busy)
                    }
                    Text("切换平台会清空本次图片会话。", style = MaterialTheme.typography.bodySmall)
                    if (runware) {
                        OutlinedTextField(state.runwareKey, onRunwareKey, label = { Text("Runware API Key") },
                            singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation())
                        Row {
                            TextButton(onClick = onSaveRunwareKey, enabled = !state.busy) { Text("保存 Key") }
                            TextButton(onClick = onClearRunwareKey, enabled = !state.busy) { Text("清除 Key") }
                        }
                        Text("Muse Image · 约 $0.01/张，实际以平台计费为准。", style = MaterialTheme.typography.bodySmall)
                    } else {
                        OutlinedTextField(state.account, onAccount, label = { Text("Account ID") }, singleLine = true,
                            enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(state.token, onToken, label = { Text("API Token") }, singleLine = true,
                            enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                        Row {
                            TextButton(onClick = onSaveSettings, enabled = !state.busy) { Text("保存设置") }
                            TextButton(onClick = onClearSettings, enabled = !state.busy) { Text("清除 Token") }
                        }
                        Text("免费额度每日 10,000 Neurons，北京时间 08:00 重置。实时剩余额度暂不可读取，请以控制台为准。", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dash.cloudflare.com/")))
                        }) { Text("查看官方用量") }
                        Text("请保持 Free 套餐。已开通付费的账号，超额计费由平台控制。", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("点击生成才会向所选平台发送提示词和参考图。不会自动重试或切换平台。参考图上传前缩小至最长边 511 像素，人物长相不保证完全一致。", style = MaterialTheme.typography.bodySmall)
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
                    Text("$providerName ▾", maxLines = 1, style = MaterialTheme.typography.titleSmall)
                }
                TextButton(onClick = onNewSession, enabled = !state.busy) { Text("新会话") }
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
                        PhotoPreview(turn.image, "第 ${index + 1} 次生成结果")
                        Row {
                            TextButton(onClick = { onUseTurn(index) }, enabled = !state.busy) { Text("继续编辑") }
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
                        if (state.busy) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("正在处理…", style = MaterialTheme.typography.bodySmall)
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
                                TextButton(onClick = onChoose, enabled = !state.busy, contentPadding = PaddingValues(0.dp)) { Text("更换") }
                            }
                            TextButton(onClick = onRemove, enabled = !state.busy) { Text("移除") }
                        }
                    }
                    if (state.result != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.continueEditing, onCheckedChange = onContinue, enabled = !state.busy)
                            Text("继续编辑上一张结果", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(state.prompt, onPrompt, placeholder = { Text("描述画面或修改要求…") },
                        minLines = 1, maxLines = 4, enabled = !state.busy,
                        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            TextButton(onClick = { showAttachment = true }, enabled = !state.busy,
                                modifier = Modifier.semantics { contentDescription = "添加图片或姿势" }) { Text("+") }
                            DropdownMenu(expanded = showAttachment, onDismissRequest = { showAttachment = false }) {
                                DropdownMenuItem(text = { Text("添加参考图") }, onClick = { showAttachment = false; onChoose() })
                                DropdownMenuItem(text = { Text("选择姿势") }, onClick = { showAttachment = false; showPoses = true })
                            }
                        }
                        TextButton(onClick = { showPoses = true }, enabled = !state.busy) { Text("姿势") }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = onGenerate, enabled = !state.busy && hasKey && state.prompt.isNotBlank(),
                            shape = RoundedCornerShape(24.dp)) { Text(if (state.busy) "处理中" else "生成 ↑") }
                    }
                }
            }
        }
    }
}
