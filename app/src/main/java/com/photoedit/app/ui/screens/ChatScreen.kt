package com.photoedit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.photoedit.app.domain.ChatProvider
import com.photoedit.app.domain.ChatModels
import com.photoedit.app.domain.ChatMode
import com.photoedit.app.domain.NovelMemory
import com.photoedit.app.viewmodel.ChatUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onKey: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onInput: (String) -> Unit,
    onMode: (ChatMode) -> Unit,
    onSend: () -> Unit,
    onClearChat: () -> Unit,
    onRegenerate: () -> Unit,
    canUsePrompt: Boolean,
    onUsePrompt: (String) -> Unit,
    onProvider: (ChatProvider) -> Unit,
    onModel: (String) -> Unit,
    onModelDraft: (String) -> Unit,
    onSavePromptRules: (String) -> Unit,
    onResetPromptRules: () -> Unit,
    onSaveNovelMemory: (NovelMemory) -> Unit,
    onSummarize: () -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var settingsProvider by remember { mutableStateOf<ChatProvider?>(null) }
    var rulesOpen by remember { mutableStateOf(false) }
    var rulesDraft by remember(state.promptRules, rulesOpen) { mutableStateOf(state.promptRules) }
    var memoryEditor by remember { mutableStateOf<String?>(null) }
    var memoryDraft by remember { mutableStateOf("") }
    var detailsExpanded by remember { mutableStateOf(false) }
    var novelToolsOpen by remember { mutableStateOf(false) }
    var savingMemory by remember { mutableStateOf(false) }
    fun editMemory(kind: String, text: String) {
        memoryDraft = text
        memoryEditor = kind
    }
    LaunchedEffect(state.busy, savingMemory) {
        if (savingMemory && !state.busy) {
            val saved = if (memoryEditor == "setting") state.novelMemory.setting else state.novelMemory.summary
            if (saved == memoryDraft) memoryEditor = null
            savingMemory = false
        }
    }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.messages.size, state.busy, state.notice) {
        val itemCount = state.messages.size.coerceAtLeast(1) +
            (if (state.busy) 1 else 0) + (if (state.notice != null) 1 else 0)
        listState.animateScrollToItem(itemCount - 1)
    }

    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val composerMaxHeight = maxHeight * 0.4f
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AI 助手", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${state.provider.label} · ${ChatModels.displayName(state.provider, state.model).removeSuffix(" · ${state.provider.label}")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    TextButton(onClick = onClearChat, enabled = !state.busy) { Text(if (state.novelMode) "清空本章" else "新对话") }
                    TextButton(onClick = { settingsOpen = true }, enabled = !state.busy) { Text("设置") }
                }
                if (detailsExpanded) {
                    val priceGuide = ChatModels.priceGuide(state.provider, state.model)
                    Text("模型 ID：${state.model}", style = MaterialTheme.typography.labelSmall)
                    Text(priceGuide?.let { "每 1 万汉字估算：输入 ${it.inputPerTenThousandChars} · 输出 ${it.outputPerTenThousandChars}" }
                        ?: "当前模型价格暂无可靠数据",
                        style = MaterialTheme.typography.labelSmall)
                    priceGuide?.let { Text(it.note, style = MaterialTheme.typography.labelSmall) }
                    Text(when (state.provider) {
                        ChatProvider.CLOUDFLARE -> "额度：实时剩余请查看 Cloudflare 控制台"

                        ChatProvider.APIPOD -> "余额：请在 APIPod 控制台查看"
                        ChatProvider.UNCENSOREDLLM -> "试用额度及余额：请在 UncensoredLLM 控制台查看"

                    }, style = MaterialTheme.typography.labelSmall)
                }
            }
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChatMode.entries.forEach { mode ->
                    FilterChip(selected = state.mode == mode, onClick = { onMode(mode) },
                        label = { Text(mode.label) }, enabled = !state.busy)
                }
                TextButton(onClick = { detailsExpanded = !detailsExpanded }) {
                    Text(if (detailsExpanded) "收起费用" else "模型费用")
                }
            }
            HorizontalDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.messages.isEmpty()) item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (state.promptMode) "提示词扩写与优化" else if (state.novelMode) "从一个主题开始写小说" else "开始新对话",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (state.promptMode)
                                "输入一段简单想法，我会将它整理、扩写成完整且可直接复制使用的图片提示词。这里只生成文字，不会生成图片。"
                            else if (state.novelMode)
                                "先聊主题、背景和人物，让 AI 帮你构思。确定后保存小说设定，再开始写正文。每章结束点“总结本章”，检查后保存为前情总结。"
                            else
                                "可以直接提问，或切换到“生成提示词”模式，让我帮你整理图片创意。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.promptMode) {
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "例如：描述人物、服饰、动作、场景，或粘贴一段已有提示词让我优化。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    TextButton(onClick = { clipboard.setText(AnnotatedString(message.text)) }) { Text("复制") }
                                    if (state.novelMode) {
                                        TextButton(onClick = { editMemory("setting", message.text) }, enabled = !state.busy) { Text("存为设定") }
                                        TextButton(onClick = { editMemory("summary", message.text) }, enabled = !state.busy) { Text("存为前情") }
                                    }
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
                            Text(if (state.input.isBlank() && state.messages.isEmpty()) "正在准备聊天服务…" else "正在生成…可切换到其他应用，后台将继续运行", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                state.notice?.let { notice ->
                    item {
                        Text(notice, modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (!state.busy && notice.contains("可用通道")) TextButton(onClick = { settingsOpen = true }) { Text("检查模型设置") }
                    }
                }
            }

            Surface(tonalElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().heightIn(max = composerMaxHeight)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    if (state.novelMode) {
                        TextButton(onClick = { novelToolsOpen = !novelToolsOpen }) {
                            Text(if (novelToolsOpen) "收起小说资料" else "小说资料与总结")
                        }
                        if (novelToolsOpen) {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editMemory("setting", state.novelMemory.setting) }, enabled = !state.busy) {
                                    Text("小说设定 · " + if (state.novelMemory.setting.isBlank()) "未保存" else "已保存")
                                }
                                OutlinedButton(onClick = { editMemory("summary", state.novelMemory.summary) }, enabled = !state.busy) {
                                    Text("前情总结 · " + if (state.novelMemory.summary.isBlank()) "未保存" else "已保存")
                                }
                                TextButton(onClick = onSummarize, enabled = !state.busy && state.token.isNotBlank() && state.messages.any { it.role == "assistant" }) { Text("总结本章") }
                            }
                            Text("总结会发送一次模型请求；清空本章保留资料。对话仅本次会话保留，请另存正文。",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (state.promptMode) TextButton(onClick = { rulesOpen = true }, enabled = !state.busy) { Text("扩写规则") }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.input, onValueChange = onInput,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(if (state.promptMode) "输入创意或粘贴提示词…" else if (state.novelMode) "聊主题，或写下这一章的想法…" else "发送消息…") },
                            minLines = 1, maxLines = 3, enabled = !state.busy,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Button(onClick = onSend,
                            enabled = !state.busy && state.input.isNotBlank() && state.token.isNotBlank(),
                            modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(if (state.busy) "等待…" else if (state.promptMode) "生成" else "发送")
                        }
                    }
                    if (state.token.isBlank()) Text("请先在设置中保存 Key",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                }
            }
        }
    }

    if (memoryEditor != null) {
        val isSetting = memoryEditor == "setting"
        AlertDialog(
            onDismissRequest = { if (!savingMemory) memoryEditor = null },
            title = { Text(if (isSetting) "小说设定" else "累计前情总结") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                    Text(if (isSetting) "保存主题、背景、人物和写作要求。只在小说模式发送给当前模型。"
                        else "用最新累计总结替换旧内容。保留重要人物、进展和伏笔，避免逐章堆叠。",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { clipboard.getText()?.text?.let { memoryDraft = it } }, enabled = !savingMemory) { Text("粘贴并替换草稿") }
                    OutlinedTextField(value = memoryDraft, onValueChange = { memoryDraft = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 6, maxLines = 14, enabled = !savingMemory,
                        placeholder = { Text("粘贴 AI 整理的内容，也可以直接编辑") })
                    Text("保存到本机；留空保存可清除。", style = MaterialTheme.typography.labelSmall)
                    if (savingMemory || state.notice != null) Text(state.notice ?: "正在保存…", style = MaterialTheme.typography.bodySmall)
                }
            },
            dismissButton = { TextButton(onClick = { memoryEditor = null }, enabled = !savingMemory) { Text("取消") } },
            confirmButton = { TextButton(onClick = {
                savingMemory = true
                onSaveNovelMemory(if (isSetting) state.novelMemory.copy(setting = memoryDraft) else state.novelMemory.copy(summary = memoryDraft))
            }, enabled = !state.busy && !savingMemory) { Text("保存") } },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.94f)
        )
    }

    if (rulesOpen) {
        AlertDialog(
            onDismissRequest = { rulesOpen = false },
            title = { Text("自定义扩写规则") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                    Text("修改后保存到本机，下一次生成提示词时生效；当前聊天记录和输入草稿都会保留。", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = rulesDraft, onValueChange = { rulesDraft = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 7, maxLines = 16,
                        label = { Text("扩写规则") }, supportingText = { Text("${rulesDraft.length} 字符") })
                }
            },
            dismissButton = { TextButton(onClick = { rulesOpen = false }) { Text("取消") } },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onResetPromptRules(); rulesOpen = false }, enabled = !state.busy) { Text("恢复默认") }
                    TextButton(onClick = { onSavePromptRules(rulesDraft); rulesOpen = false },
                        enabled = !state.busy && rulesDraft.isNotBlank()) { Text("保存") }
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.94f)
        )
    }

    if (settingsOpen) {
        Dialog(
            onDismissRequest = { settingsOpen = false; settingsProvider = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler(enabled = settingsProvider != null) { settingsProvider = null }
            val overviewScroll = rememberScrollState()
            val platformScroll = key(settingsProvider) { rememberScrollState() }
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (settingsProvider != null) {
                            TextButton(onClick = { settingsProvider = null }) { Text("返回") }
                        }
                        Text(settingsProvider?.label ?: "平台与模型", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { settingsOpen = false; settingsProvider = null }) { Text("完成") }
                    }
                    HorizontalDivider()
                    Column(
                        Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(if (settingsProvider == null) overviewScroll else platformScroll).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (settingsProvider == null) {
                            Text("按平台配置", style = MaterialTheme.typography.titleMedium)
                            Text("每个平台分别配置密钥和模型。切换平台会开始新对话，小说资料保留。",
                                style = MaterialTheme.typography.bodySmall)
                            ChatProvider.entries.forEach { provider ->
                                OutlinedCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(provider.label, style = MaterialTheme.typography.titleMedium)
                                        if (state.provider == provider) {
                                            Text("当前使用 · ${ChatModels.displayName(provider, state.model)}",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text("预设模型：" + ChatModels.presets(provider).joinToString("、") { it.name },
                                            style = MaterialTheme.typography.bodySmall)
                                        OutlinedButton(onClick = {
                                            settingsProvider = provider
                                            onProvider(provider)
                                        }, enabled = !state.busy) {
                                            Text(if (state.provider == provider) "查看配置" else "切换并配置")
                                        }
                                    }
                                }
                            }
                        } else if (settingsProvider != state.provider) {
                            Text(if (state.busy) "正在读取平台配置…" else "平台切换未完成，请返回重试。")
                            state.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        } else {
                            Text("1. 平台密钥", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when (state.provider) {
                                    ChatProvider.CLOUDFLARE -> "填写 Cloudflare 控制台创建的 API Token（访问令牌）。账户 ID 由应用读取已保存配置，与下面的模型 ID 不同，此处无需填写。GLM-4.7-Flash 按 Workers AI Neurons 额度计费。"

                                    ChatProvider.APIPOD -> "直接复用图片生成设置中已保存的 APIPod Key。聊天与图片生成共用同一 Key；清除会同时影响两项功能。"
                                    ChatProvider.UNCENSOREDLLM -> "使用单独注册的 UncensoredLLM API Key，按 Token 计费；试用额度以控制台为准。此处是平台提供的低拒答衍生版，仍遵循平台规则。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.provider == ChatProvider.UNCENSOREDLLM) {
                                TextButton(onClick = { uriHandler.openUri("https://uncensoredllm.ai/") }) { Text("注册 / 获取 Key") }
                                TextButton(onClick = { uriHandler.openUri("https://uncensoredllm.ai/pricing") }) { Text("查看实时价格") }
                            }
                            OutlinedTextField(
                                value = state.token,
                                onValueChange = onKey,
                                label = { Text(if (state.provider == ChatProvider.CLOUDFLARE) "访问令牌（API Token）" else "平台密钥（API Key）") },
                                supportingText = { Text("从当前平台控制台复制；仅加密保存在本机。保存不代表已验证接口可用。") },
                                singleLine = true,
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onSaveKey, enabled = !state.busy && state.token.isNotBlank()) { Text("保存密钥") }
                                TextButton(onClick = onClearKey, enabled = !state.busy) { Text("清除密钥") }
                            }
                            HorizontalDivider()
                            Text("2. 选择模型", style = MaterialTheme.typography.titleMedium)
                            Text("点选预设即可，无需填写 ID。更换模型会保存选择并开始新对话，小说资料保留。",
                                style = MaterialTheme.typography.bodySmall)
                            ModelSelector(state, onModel, onModelDraft)
                            Text(
                                when (state.provider) {
                                    ChatProvider.CLOUDFLARE -> "Cloudflare Token 会加密保存在本机。"

                                    ChatProvider.APIPOD -> "APIPod 聊天与图片生成共用同一个 Key；在这里保存或清除会同步影响图片生成。"
                                    ChatProvider.UNCENSOREDLLM -> "Key 仅加密保存在本机，独立于其他平台；仅主动发送、总结或重新生成时发送文字。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            state.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text("预设模型不代表当前 Key 一定可用；以平台控制台的模型权限与通道状态为准。", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelSelector(
    state: ChatUiState,
    onModel: (String) -> Unit,
    onDraft: (String) -> Unit
) {
    var advanced by remember(state.provider) { mutableStateOf(false) }
    Text("当前模型：${ChatModels.displayName(state.provider, state.model)}", style = MaterialTheme.typography.bodyMedium)
    state.models.forEach { model ->
        Row(
            Modifier.fillMaxWidth().selectable(
                selected = state.model == model.id,
                enabled = !state.busy,
                role = Role.RadioButton,
                onClick = { if (state.model != model.id) onModel(model.id) }
            ).heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = state.model == model.id, onClick = null, enabled = !state.busy)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(model.name)
                ChatModels.priceGuide(state.provider, model.id)?.let { price ->
                    Text("每万汉字估算：输入 ${price.inputPerTenThousandChars} · 输出 ${price.outputPerTenThousandChars}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    TextButton(onClick = { advanced = !advanced }) {
        Text(if (advanced) "收起高级设置" else "高级设置：其他模型")
    }
    if (advanced) {
        Text("模型 ID 是平台为模型指定的接口名称，不是账号 ID、密钥或网址。只有使用预设以外的模型时，才需从当前平台的模型文档复制完整 ID。",
            style = MaterialTheme.typography.bodySmall)
        SelectionContainer {
            Text("当前模型 ID：${state.model}", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = state.modelDraft,
            onValueChange = onDraft,
            label = { Text("其他模型的 ID（可选）") },
            placeholder = { Text("例如：${state.provider.defaultModel}") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.busy
        )
        TextButton(
            onClick = { onModel(state.modelDraft) },
            enabled = !state.busy && state.modelDraft.isNotBlank()
        ) { Text("使用并保存此模型") }
    }
}
