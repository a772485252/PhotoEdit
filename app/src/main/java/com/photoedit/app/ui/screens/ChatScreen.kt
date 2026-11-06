package com.photoedit.app.ui.screens

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.photoedit.app.domain.ChatProvider
import com.photoedit.app.viewmodel.ChatUiState

@Composable
fun ChatScreen(
    state: ChatUiState,
    onKey: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onInput: (String) -> Unit,
    onMode: (Boolean) -> Unit,
    onSend: () -> Unit,
    onClearChat: () -> Unit,
    onRegenerate: () -> Unit,
    canUsePrompt: Boolean,
    onUsePrompt: (String) -> Unit,
    onProvider: (ChatProvider) -> Unit,
    onModel: (String) -> Unit,
    onModelDraft: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onAllowPaid: (Boolean) -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(state.messages.size, state.busy) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("提示词助手", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${state.provider.label} · ${state.model.substringAfterLast('/')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            TextButton(onClick = onClearChat, enabled = !state.busy) { Text("新对话") }
            TextButton(onClick = { settingsOpen = true }, enabled = !state.busy) { Text("设置") }
        }
        HorizontalDivider()

        if (state.messages.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (state.promptMode) "提示词扩写与优化" else "开始新对话",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (state.promptMode)
                        "输入一段简单想法，我会将它整理、扩写成完整且可直接复制使用的图片提示词。这里只生成文字，不会生成图片。"
                    else
                        "可以直接提问，或切换到“生成提示词”模式，让我帮你整理图片创意。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.busy) {
                    Spacer(Modifier.height(22.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            if (state.input.isNotBlank()) "正在生成提示词，请稍候…" else "正在准备聊天服务…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (state.promptMode) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "例如：描述人物、服饰、动作、场景，或粘贴一段已有提示词让我优化。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(state.messages) { index, message ->
                    val isUser = message.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Column(Modifier.widthIn(max = 340.dp), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                            Text(
                                if (isUser) "你" else "PhotoEdit AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp, topEnd = 18.dp,
                                    bottomStart = if (isUser) 18.dp else 5.dp,
                                    bottomEnd = if (isUser) 5.dp else 18.dp
                                ),
                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                SelectionContainer {
                                    Text(
                                        message.text,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!isUser) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    TextButton(onClick = { clipboard.setText(AnnotatedString(message.text)) }) { Text("复制") }
                                    if (state.promptMode) {
                                        TextButton(
                                            onClick = { onUsePrompt(message.text) },
                                            enabled = canUsePrompt && message.text.length <= 4000
                                        ) { Text("带入生图") }
                                    }
                                    if (index == state.messages.lastIndex && !state.busy) {
                                        TextButton(onClick = onRegenerate) { Text("重新生成") }
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.busy) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("正在思考…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        state.notice?.let {
            Text(
                it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Surface(tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = state.promptMode,
                        onClick = { onMode(true) },
                        label = { Text("生成提示词") },
                        enabled = !state.busy
                    )
                    FilterChip(
                        selected = !state.promptMode,
                        onClick = { onMode(false) },
                        label = { Text("普通聊天") },
                        enabled = !state.busy
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { settingsOpen = true }, enabled = !state.busy) { Text("模型设置") }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = onInput,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (state.promptMode) "输入创意或粘贴需要优化的提示词…" else "发送消息…") },
                        minLines = 1,
                        maxLines = 4,
                        enabled = !state.busy,
                        shape = RoundedCornerShape(22.dp)
                    )
                    Button(
                        onClick = onSend,
                        enabled = !state.busy && state.input.isNotBlank() && state.token.isNotBlank(),
                        modifier = Modifier.height(54.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text(if (state.busy) "处理中…" else if (state.promptMode) "生成提示词" else "发送") }
                }
                Text(
                    "聊天仅发送文字和最近对话，不会上传参考图；带入生图页也不会自动生成图片。",
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("模型与 API 设置") },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("聊天服务", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.provider == ChatProvider.CLOUDFLARE,
                            onClick = { onProvider(ChatProvider.CLOUDFLARE) },
                            label = { Text("Cloudflare · GLM") },
                            enabled = !state.busy
                        )
                        FilterChip(
                            selected = state.provider == ChatProvider.VENICE,
                            onClick = { onProvider(ChatProvider.VENICE) },
                            label = { Text("Venice · Uncensored") },
                            enabled = !state.busy
                        )
                        FilterChip(
                            selected = state.provider == ChatProvider.APIPOD,
                            onClick = { onProvider(ChatProvider.APIPOD) },
                            label = { Text("APIPod · Grok") },
                            enabled = !state.busy
                        )
                    }
                    Text(
                        when (state.provider) {
                            ChatProvider.CLOUDFLARE -> "使用本机已保存的 Cloudflare Token 和 Account ID。GLM-4.7-Flash 按 Workers AI Neurons 额度计费。"
                            ChatProvider.VENICE -> "使用与 Venice 图片生成共用的 API Key。API 按所选模型 Token 价格计费。"
                            ChatProvider.APIPOD -> "直接复用图片生成设置中已保存的 APIPod Key。聊天与图片生成共用同一 Key；清除会同时影响两项功能。"
                            else -> "使用当前服务商的 API Key。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = state.token,
                        onValueChange = onKey,
                        label = { Text("${state.provider.label} API Key") },
                        singleLine = true,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onSaveKey, enabled = !state.busy && state.token.isNotBlank()) { Text("保存 Key") }
                        TextButton(onClick = onClearKey, enabled = !state.busy) { Text("清除当前 Key") }
                    }
                    HorizontalDivider()
                    Text("模型选择", style = MaterialTheme.typography.titleSmall)
                    ModelSelector(state, onModel, onModelDraft, onRefreshModels, onAllowPaid)
                    Text(
                        when (state.provider) {
                            ChatProvider.CLOUDFLARE -> "Cloudflare Token 会加密保存在本机。"
                            ChatProvider.VENICE -> "Venice 聊天与图片生成共用同一个 Key；在这里清除也会同时影响 Venice 图片生成。"
                            ChatProvider.APIPOD -> "APIPod 聊天与图片生成共用同一个 Key；在这里保存或清除会同步影响图片生成。"
                            else -> "当前服务商凭据保存在本机。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { settingsOpen = false }) { Text("完成") } },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.94f)
        )
    }
}

@Composable
private fun ModelSelector(
    state: ChatUiState,
    onModel: (String) -> Unit,
    onDraft: (String) -> Unit,
    onRefresh: () -> Unit,
    onAllowPaid: (Boolean) -> Unit
) {
    var expanded by remember(state.provider) { mutableStateOf(false) }
    var query by remember(state.provider) { mutableStateOf("") }
    val router = state.provider == ChatProvider.OPENROUTER
    Text("当前模型：${state.model}", style = MaterialTheme.typography.bodyMedium)
    if (router) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.allowPaid, onCheckedChange = onAllowPaid, enabled = !state.busy)
            Text("显示并允许付费模型（按平台计费）", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "付费开关仅本次会话有效。关闭时只允许 :free 模型或 openrouter/free。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索模型名称或 ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.busy
        )
    }
    val available = state.models.filter {
        (!router || state.allowPaid || (it.free == true && it.id.endsWith(":free"))) &&
            (it.name.contains(query, true) || it.id.contains(query, true))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = !state.busy) { Text("选择模型（${available.size}）") }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                available.take(100).forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                model.name + "\n" + model.id + if (router) {
                                    if (model.free == true) " · 免费" else " · 付费／请查看平台价格"
                                } else if (state.provider == ChatProvider.GROQ) " · 额度按 Groq 账号套餐" else if (state.provider == ChatProvider.VENICE) " · Venice API 按 Token 计费" else if (state.provider == ChatProvider.APIPOD) " · APIPod 按 Token 计费" else " · Cloudflare Workers AI"
                            )
                        },
                        onClick = { expanded = false; onModel(model.id) }
                    )
                }
                if (available.isEmpty()) {
                    DropdownMenuItem(text = { Text("无匹配模型，可刷新或手动填写 ID") }, onClick = {}, enabled = false)
                }
            }
        }
        if (router) {
            TextButton(onClick = onRefresh, enabled = !state.busy && state.token.isNotBlank()) { Text("刷新列表") }
        }
    }
    OutlinedTextField(
        value = state.modelDraft,
        onValueChange = onDraft,
        label = { Text("自定义模型 ID") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !state.busy
    )
    TextButton(
        onClick = { onModel(state.modelDraft) },
        enabled = !state.busy && state.modelDraft.isNotBlank()
    ) { Text("使用此模型并保存") }
}
