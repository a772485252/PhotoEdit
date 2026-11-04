package com.photoedit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.photoedit.app.viewmodel.ChatUiState
import com.photoedit.app.domain.ChatProvider

@Composable
fun ChatScreen(state: ChatUiState, onKey: (String) -> Unit, onSaveKey: () -> Unit, onClearKey: () -> Unit,
    onInput: (String) -> Unit, onMode: (Boolean) -> Unit, onSend: () -> Unit, onClearChat: () -> Unit,
    canUsePrompt: Boolean, onUsePrompt: (String) -> Unit,
    onProvider: (ChatProvider) -> Unit, onModel: (String) -> Unit,
    onModelDraft: (String) -> Unit, onRefreshModels: () -> Unit, onAllowPaid: (Boolean) -> Unit) {
    val scroll = rememberScrollState()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue) }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("提示词助手", style = MaterialTheme.typography.headlineSmall)
        Text("聊天额度与 Cloudflare 生图独立，各平台分别填写 Key。")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatProvider.entries.forEach { provider ->
                FilterChip(state.provider == provider, { onProvider(provider) },
                    label = { Text(provider.label) }, enabled = !state.busy)
            }
        }
        if (state.provider == ChatProvider.OPENROUTER) {
            Text("免费模型有每日和速率限制，高峰可能不可用。不会自动改用付费模型。", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Groq Free 账号有独立额度；付费账号按平台设置计费。", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(state.token, onKey, label = { Text("${state.provider.label} API Key") }, singleLine = true,
            enabled = !state.busy, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSaveKey, enabled = !state.busy) { Text("保存 Key") }
            TextButton(onClick = onClearKey, enabled = !state.busy) { Text("清除 Key") }
        }
        ModelSelector(state, onModel, onModelDraft, onRefreshModels, onAllowPaid)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(state.promptMode, { onMode(true) }, label = { Text("生成提示词") }, enabled = !state.busy)
            FilterChip(!state.promptMode, { onMode(false) }, label = { Text("普通聊天") }, enabled = !state.busy)
        }
        Text("只向所选平台发送文字和最近对话，不上传参考图。切换平台、模型或模式会开始新对话，草稿保留。", style = MaterialTheme.typography.bodySmall)
        state.messages.forEach { message ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (message.role == "user") "你" else "助手", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(message.text) }
                    if (message.role == "assistant") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(message.text)) }) { Text("复制") }
                        if (state.promptMode) TextButton(onClick = { onUsePrompt(message.text) }, enabled = canUsePrompt && message.text.length <= 4000) { Text("带入生图页") }
                    }
                }
            }
        }
        if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在处理…") }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        OutlinedTextField(state.input, onInput, label = { Text(if (state.promptMode) "描述你想生成的画面" else "输入消息") }, minLines = 3,
            enabled = !state.busy, modifier = Modifier.fillMaxWidth())
        Button(onClick = onSend, enabled = !state.busy && state.input.isNotBlank() && state.token.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (state.promptMode) "生成／修改提示词" else "发送")
        }
        TextButton(onClick = onClearChat, enabled = !state.busy) { Text("开始新对话") }
        Text("带入生图页只填入文字，不会自动生成图片。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModelSelector(state: ChatUiState, onModel: (String) -> Unit, onDraft: (String) -> Unit,
    onRefresh: () -> Unit, onAllowPaid: (Boolean) -> Unit) {
    var expanded by remember(state.provider) { mutableStateOf(false) }
    var query by remember(state.provider) { mutableStateOf("") }
    val router = state.provider == ChatProvider.OPENROUTER
    Text("当前模型：${state.model}", style = MaterialTheme.typography.bodyMedium)
    if (router) {
        Row {
            Checkbox(checked = state.allowPaid, onCheckedChange = onAllowPaid, enabled = !state.busy)
            Text("显示并允许付费模型（按平台计费）", modifier = Modifier.padding(top = 12.dp))
        }
        Text("付费开关仅本次会话有效。关闭时只允许 :free 模型或 openrouter/free。",
            style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(query, { query = it }, label = { Text("搜索模型名称或 ID") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy)
    }
    val available = state.models.filter {
        (!router || state.allowPaid || (it.free == true && it.id.endsWith(":free"))) &&
            (it.name.contains(query, true) || it.id.contains(query, true))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = !state.busy) { Text("选择模型（${available.size}）") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp)) {
                available.take(100).forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.name + "\n" + model.id + if (router) {
                            if (model.free == true) " · 免费" else " · 付费／请查看平台价格"
                        } else " · 额度按 Groq 账号套餐") },
                        onClick = { expanded = false; onModel(model.id) })
                }
                if (available.size > 100) DropdownMenuItem(text = { Text("请搜索以缩小结果范围") }, onClick = {}, enabled = false)
                if (available.isEmpty()) DropdownMenuItem(text = { Text("无匹配模型，可刷新或手动填写 ID") }, onClick = {}, enabled = false)
            }
        }
        if (router) TextButton(onClick = onRefresh, enabled = !state.busy && state.token.isNotBlank()) { Text("刷新列表") }
    }
    OutlinedTextField(state.modelDraft, onDraft, label = { Text("自定义模型 ID") },
        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy)
    TextButton(onClick = { onModel(state.modelDraft) }, enabled = !state.busy && state.modelDraft.isNotBlank()) { Text("使用此模型并保存") }
    Text("平台和模型选择会自动保存。预置列表不代表实时可用；刷新列表需要 OpenRouter Key，不会发送聊天。",
        style = MaterialTheme.typography.bodySmall)
}
