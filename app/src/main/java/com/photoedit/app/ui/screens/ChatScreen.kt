package com.photoedit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.photoedit.app.viewmodel.ChatUiState

@Composable
fun ChatScreen(state: ChatUiState, onKey: (String) -> Unit, onSaveKey: () -> Unit, onClearKey: () -> Unit,
    onInput: (String) -> Unit, onMode: (Boolean) -> Unit, onSend: () -> Unit, onClearChat: () -> Unit,
    canUsePrompt: Boolean, onUsePrompt: (String) -> Unit) {
    val scroll = rememberScrollState()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue) }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("提示词助手 · Groq", style = MaterialTheme.typography.headlineSmall)
        Text("GPT-OSS 20B · 独立免费额度，不消耗 Cloudflare 生图额度。")
        Text("需要单独注册 Groq 并创建 API Key。保持 Free 套餐；额度受速率、每日请求数和 Token 数限制。", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(state.token, onKey, label = { Text("Groq API Key（不是 Cloudflare Token）") }, singleLine = true,
            enabled = !state.busy, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSaveKey, enabled = !state.busy) { Text("保存 Key") }
            TextButton(onClick = onClearKey, enabled = !state.busy) { Text("清除 Key") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(state.promptMode, { onMode(true) }, label = { Text("生成提示词") }, enabled = !state.busy)
            FilterChip(!state.promptMode, { onMode(false) }, label = { Text("普通聊天") }, enabled = !state.busy)
        }
        Text("只向 Groq 发送文字和最近对话，不上传参考图。对话仅保留在当前会话，切换模式会清空。", style = MaterialTheme.typography.bodySmall)
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
