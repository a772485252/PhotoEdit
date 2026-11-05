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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.photoedit.app.domain.ApiPodRules
import com.photoedit.app.domain.VeniceRules
import com.photoedit.app.domain.HordeRules
import com.photoedit.app.domain.OutfitCatalog
import com.photoedit.app.domain.PromptComposer
import com.photoedit.app.domain.PosePrompts
import com.photoedit.app.domain.TwoPanelPosePrompts
import com.photoedit.app.ui.components.PhotoPreview
import com.photoedit.app.ui.components.PosePicker
import com.photoedit.app.viewmodel.AiUiState

@Composable
fun AiScreen(state: AiUiState, onAccount: (String) -> Unit, onToken: (String) -> Unit,
             onPrompt: (String) -> Unit, onSaveSettings: () -> Unit, onClearSettings: () -> Unit,
             onChoose: () -> Unit, onRemove: () -> Unit, onGenerate: () -> Unit, onSaveImage: () -> Unit,
 onNewSession: () -> Unit, onUseTurn: (Int) -> Unit, onContinue: (Boolean) -> Unit,
 onProvider: (String) -> Unit, onRunwareKey: (String) -> Unit, onSaveRunwareKey: () -> Unit, onClearRunwareKey: () -> Unit,
 onHordeKey: (String) -> Unit, onSaveHordeKey: () -> Unit, onClearHordeKey: () -> Unit,
 onHordeModel: (String) -> Unit, onHordeDenoise: (Double) -> Unit, onHordeNsfw: (Boolean) -> Unit, onRefreshHordeModels: () -> Unit,
 onStopHordeWaiting: () -> Unit, onCancelHordeTask: () -> Unit,
 onApiPodKey: (String) -> Unit, onSaveApiPodKey: () -> Unit, onClearApiPodKey: () -> Unit,
 onApiPodPro: (Boolean) -> Unit, onStopApiPodWaiting: () -> Unit, onAbandonApiPodTracking: () -> Unit,
 onVeniceKey: (String) -> Unit, onSaveVeniceKey: () -> Unit, onClearVeniceKey: () -> Unit,
 onAcknowledgeVeniceOutcome: () -> Unit) {
    val context = LocalContext.current
    val history = rememberLazyListState()
    var showSettings by remember { mutableStateOf(false) }
    var showPoses by remember { mutableStateOf(false) }
    var showOutfits by remember { mutableStateOf(false) }
    var outfitCategory by remember { mutableStateOf("全部") }
    var twoPanelIds by remember { mutableStateOf<String?>(null) }
    var showAttachment by remember { mutableStateOf(false) }
    var showReference by remember { mutableStateOf(false) }
    var enlargedResult by remember { mutableStateOf<Bitmap?>(null) }
    val runware = state.provider == "runware"
    val horde = state.provider == "horde"
    val apiPod = state.provider == "apipod"
    val venice = state.provider == "venice"
    val pendingTask = state.hordeTaskPending || state.apiPodPending
    val veniceUnknown = venice && state.veniceOutcomeUnknown
    val effectiveReference = (state.continueEditing && state.result != null) || state.reference != null
    val apiPodModel = ApiPodRules.model(effectiveReference, state.apiPodPro)
    val apiPodEstimate = ApiPodRules.estimate(effectiveReference, state.apiPodPro)
    var confirmAbandon by remember { mutableStateOf(false) }
    val editingEnabled = !state.busy && !pendingTask && !state.veniceOutcomeUnknown
    var confirmVeniceOutcome by remember { mutableStateOf(false) }
    var showHordeModels by remember { mutableStateOf(false) }
    val providerName = when {
        venice -> "Venice · " + VeniceRules.model(effectiveReference)
        apiPod -> "APIPod · 付费"
        horde -> "AI Horde · 免费队列"
        runware -> "Runware · Muse Image"
        else -> "Cloudflare · FLUX.2 Klein"
    }
    val hasKey = when { venice -> state.veniceKey.isNotBlank(); apiPod -> state.apiPodKey.isNotBlank(); horde -> true; runware -> state.runwareKey.isNotBlank(); else -> state.token.isNotBlank() }

    LaunchedEffect(state.turns.size) {
        // One footer item always exists, including in an empty conversation.
        history.animateScrollToItem(state.turns.size)
    }
    if (showPoses && editingEnabled) {
        PosePicker(onDismiss = { showPoses = false }, onSelect = {
            onPrompt(PromptComposer.withPose(state.prompt, it))
            twoPanelIds = null
            showPoses = false
        })
    }
    if (showOutfits && editingEnabled) {
        Dialog(onDismissRequest = { showOutfits = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("选择服装风格", style = MaterialTheme.typography.titleLarge)
                    Text("17 种上衣风格 · 下装随机：白色透明连裤袜 / 白色低腰蕾丝内裤；仅修改提示词", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("全部", "国风舞蹈", "宅舞", "清纯", "时尚").forEach { category ->
                            FilterChip(outfitCategory == category, { outfitCategory = category },
                                label = { Text(category) })
                        }
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(OutfitCatalog.presets.filter { outfitCategory == "全部" || it.category == outfitCategory }.size) { index ->
                            val filtered = OutfitCatalog.presets.filter { outfitCategory == "全部" || it.category == outfitCategory }
                            val outfit = filtered[index]
                            OutlinedCard(onClick = {
                                onPrompt(PromptComposer.withOutfit(state.prompt, outfit))
                                showOutfits = false
                            }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(outfit.title, style = MaterialTheme.typography.titleSmall)
                                    Text("保留该风格上衣设计；下装在两种白色款式中随机替换。", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { showOutfits = false }, modifier = Modifier.align(Alignment.End)) { Text("关闭") }
                }
            }
        }
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
    if (confirmVeniceOutcome) {
        AlertDialog(onDismissRequest = { confirmVeniceOutcome = false }, title = { Text("已核对上一笔请求？") },
            text = { Text("Venice 本次接口是同步生成，无法查询或恢复丢失的响应。请先核对平台用量。继续后再次生成会新建付费请求，不能取消或退回上一笔费用。") },
            confirmButton = { TextButton(onClick = { confirmVeniceOutcome = false; onAcknowledgeVeniceOutcome() }) { Text("已核对，允许新请求") } },
            dismissButton = { TextButton(onClick = { confirmVeniceOutcome = false }) { Text("返回核对") } })
    }
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("图片生成设置", style = MaterialTheme.typography.titleLarge)
                    Column {
                        FilterChip(venice, { onProvider("venice") }, label = { Text("Venice · 付费") }, enabled = editingEnabled)
                        FilterChip(apiPod, { onProvider("apipod") }, label = { Text("APIPod · 付费") }, enabled = editingEnabled)
                        FilterChip(horde, { onProvider("horde") }, label = { Text("AI Horde · 免费") }, enabled = editingEnabled)
                        FilterChip(!runware && !horde && !apiPod && !venice, { onProvider("cloudflare") }, label = { Text("Cloudflare") }, enabled = editingEnabled)
                        FilterChip(runware, { onProvider("runware") }, label = { Text("Runware · 付费") }, enabled = editingEnabled)
                    }
                    Text("切换平台保留草稿和参考图，清空当前平台的结果记录。", style = MaterialTheme.typography.bodySmall)
                    if (venice) {
                        Text("按次付费：文字生图 Z-Image Turbo 约 $0.01/张；单张参考图编辑 FireRed 约 $0.04/次，以平台结算为准。", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(state.veniceKey, onVeniceKey, label = { Text("Venice API Key") }, singleLine = true,
                            enabled = editingEnabled, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                        Row {
                            TextButton(onClick = onSaveVeniceKey, enabled = editingEnabled) { Text("保存 Key") }
                            TextButton(onClick = onClearVeniceKey, enabled = editingEnabled) { Text("清除 Key") }
                        }
                        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://venice.ai/settings/api"))) }) { Text("打开 Venice API 控制台") }
                        Text("本次模型：${VeniceRules.model(effectiveReference)}", style = MaterialTheme.typography.bodySmall)
                        Text("支持一张参考图或继续编辑上一张结果；参考图最长边缩至 1024，缩放后宽高均需至少 256 像素。文生图 1024 × 1024。未开启额外付费提示词增强。", style = MaterialTheme.typography.bodySmall)
                        Text("保留平台默认内容审核与成人内容模糊处理；模型规则不同，不承诺审核通过或人体完全无缺陷。参考图编辑接口仍属实验性功能。", style = MaterialTheme.typography.bodySmall)
                        Text("此接口同步返回图片，不支持任务查询。超时请先核对用量，应用不会自动重试。API 按用量付费，网页免费额度不能直接当作 API 额度。", style = MaterialTheme.typography.bodySmall)
                    } else if (apiPod) {
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
                    } else if (horde) {
                        Text("免费社区算力 · 保持 512 × 512，不自动降档；积分不足会提示，不会自动付费。", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(state.hordeKey, onHordeKey, label = { Text("个人 API Key（留空可匿名试用）") },
                            singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation())
                        Row {
                            TextButton(onClick = onSaveHordeKey, enabled = !state.busy) { Text("保存 Key") }
                            TextButton(onClick = onClearHordeKey, enabled = !state.busy) { Text("匿名模式") }
                        }
                        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aihorde.net/register"))) }) { Text("注册并获取个人 Key") }
                        Text("人像优先", style = MaterialTheme.typography.titleSmall)
                        HordeRules.portraitModels.forEach { model ->
                            FilterChip(state.hordeModel == model, { onHordeModel(model) }, label = { Text(model) }, enabled = editingEnabled)
                        }
                        OutlinedTextField(state.hordeModel, onHordeModel, label = { Text("模型名称") },
                            singleLine = true, enabled = editingEnabled, modifier = Modifier.fillMaxWidth())
                        Row {
                            TextButton(onClick = onRefreshHordeModels, enabled = !state.busy) { Text("刷新在线模型") }
                            Box {
                                TextButton(onClick = { showHordeModels = true }, enabled = editingEnabled && state.hordeModels.isNotEmpty()) { Text("选择模型") }
                                DropdownMenu(expanded = showHordeModels, onDismissRequest = { showHordeModels = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                                    state.hordeModels.forEach { model ->
                                        DropdownMenuItem(text = { Text("${model.name} · ${model.workers} 节点") },
                                            onClick = { onHordeModel(model.name); showHordeModels = false })
                                    }
                                }
                            }
                        }
                        if (state.hordeModel in HordeRules.portraitModels) {
                            Text("写真优化已开启：匹配采样参数，并补充清晰度提示与负面词；原输入文字保留。", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("参考图修改程度", style = MaterialTheme.typography.titleSmall)
                        listOf(0.35 to "保留更多原图", 0.55 to "平衡", 0.65 to "较大变化").forEach { (strength, label) ->
                            FilterChip(kotlin.math.abs(state.hordeDenoise - strength) < 0.01,
                                { onHordeDenoise(strength) }, label = { Text(label) }, enabled = editingEnabled)
                        }
                        Text("默认平衡；低强度更偏向保留原图，变化越大，人物长相越可能改变。较低强度也可能保留原图的模糊，不是人脸锁定。英文提示词通常更适合这些模型，可用提示词助手整理。", style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(state.hordeNsfw, onHordeNsfw, enabled = editingEnabled)
                            Text("允许 NSFW 请求", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("仅由接受此类请求的节点处理，可能等得更久；平台与节点内容规则仍生效。图生图不保证保脸或精确换装。", style = MaterialTheme.typography.bodySmall)
                        Text("参考图交由志愿节点处理。匿名文生图及提示词会被平台共享给 LAION；个人 Key 请求关闭主动共享。", style = MaterialTheme.typography.bodySmall)
                    } else if (runware) {
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
                    Text("点击生成才会向所选平台发送提示词和参考图。不会自动重试或切换平台。参考图会按所选平台缩小，人物长相不保证完全一致。", style = MaterialTheme.typography.bodySmall)
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
                            Text(if (horde) "免费排队生成，输入描述或点 + 添加参考图。" else "输入描述，或点 + 添加参考图。", style = MaterialTheme.typography.bodyMedium)
                            if (!hasKey) TextButton(onClick = { showSettings = true }) { Text("设置 API Key") }
                        }
                        if (apiPod && state.apiPodPending) {
                            Text("原操作已保留，继续查询不会另建新的逻辑任务。", style = MaterialTheme.typography.bodySmall)
                            if (state.busy) TextButton(onClick = onStopApiPodWaiting) { Text("停止本机等待") }
                            else TextButton(onClick = { confirmAbandon = true }) { Text("结束本地跟踪…") }
                        }
                        if (veniceUnknown) {
                            Text("上一笔结果与计费未知，请先核对平台用量；再次生成会新建付费请求。", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://venice.ai/settings/api"))) }) { Text("核对 Venice 用量") }
                            TextButton(onClick = { confirmVeniceOutcome = true }) { Text("核对后继续…") }
                        }
                        if (horde && state.hordeTaskPending) {
                            Text("当前模型：${state.hordeModel}", style = MaterialTheme.typography.bodySmall)
                            if (state.busy) TextButton(onClick = onStopHordeWaiting) { Text("停止等待（保留平台任务）") }
                            else TextButton(onClick = onCancelHordeTask) { Text("取消原任务") }
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
                        veniceUnknown -> "上一笔结果未知，核对用量后才能继续。"
                        pendingTask -> "原任务已保留，请继续查询或处理原任务。"
                        state.continueEditing && state.result != null -> "本次参考：上一张生成结果"
                        state.reference != null -> "本次参考：已选图片"
                        else -> "本次参考：无（文生图）"
                    }, style = MaterialTheme.typography.labelSmall)
                    if (apiPod) Text(apiPodModel + " · 约 $" + apiPodEstimate + "/次 · 以平台结算为准", style = MaterialTheme.typography.labelSmall)
                    if (venice) Text(VeniceRules.model(effectiveReference) + " · 约 $" + VeniceRules.estimate(effectiveReference) + "/次 · 以平台结算为准", style = MaterialTheme.typography.labelSmall)
                    if (!hasKey) TextButton(onClick = { showSettings = true }) { Text("先设置 ${if (venice) "Venice Key" else if (apiPod) "APIPod Key" else if (runware) "Runware Key" else "Cloudflare Token"}") }
                    OutlinedTextField(state.prompt, onPrompt, placeholder = { Text("描述画面或修改要求…") },
                        minLines = 1, maxLines = 4, enabled = editingEnabled,
                        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                    twoPanelIds?.let { Text("已随机选中：$it · 点击随机双宫格可重新抽取；生成时仅请求一张图片。", style = MaterialTheme.typography.labelSmall) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            TextButton(onClick = { showAttachment = true }, enabled = editingEnabled,
                                modifier = Modifier.semantics { contentDescription = "添加图片或姿势" }) { Text("+") }
                            DropdownMenu(expanded = showAttachment, onDismissRequest = { showAttachment = false }) {
                                DropdownMenuItem(text = { Text("添加参考图") }, onClick = { showAttachment = false; onChoose() })
                                DropdownMenuItem(text = { Text("选择姿势") }, onClick = { showAttachment = false; showPoses = true })
                                DropdownMenuItem(text = { Text("选择服装风格") }, onClick = { showAttachment = false; showOutfits = true })
                                DropdownMenuItem(text = { Text("随机双宫格（一次生成）") }, onClick = {
                                    showAttachment = false
                                    val (left, right) = TwoPanelPosePrompts.choose()
                                    onPrompt(PromptComposer.withTwoPanel(state.prompt, left, right))
                                    twoPanelIds = "${left.id} + ${right.id}"
                                })
                            }
                        }
                        TextButton(onClick = { showPoses = true }, enabled = editingEnabled) { Text("姿势") }
                        TextButton(onClick = { showOutfits = true }, enabled = editingEnabled) { Text("服装") }
                        TextButton(onClick = {
                            val (left, right) = TwoPanelPosePrompts.choose()
                            onPrompt(PromptComposer.withTwoPanel(state.prompt, left, right))
                            twoPanelIds = "${left.id} + ${right.id}"
                        }, enabled = editingEnabled) { Text("随机双宫格") }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = onGenerate, enabled = !state.busy && !veniceUnknown && hasKey && (pendingTask || state.prompt.isNotBlank()),
                            shape = RoundedCornerShape(24.dp)) { Text(if (state.busy) "处理中" else if (pendingTask) "继续查询" else if (veniceUnknown) "先核对用量" else if (venice) "生成 · $" + VeniceRules.estimate(effectiveReference) else if (apiPod) "生成 · $" + apiPodEstimate else "生成 ↑") }
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
