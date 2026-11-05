package com.photoedit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.photoedit.app.ui.components.PosePicker
import com.photoedit.app.domain.PosePrompts
import com.photoedit.app.ui.components.PhotoPreview
import com.photoedit.app.viewmodel.AiUiState

@Composable
fun AiScreen(state: AiUiState, onAccount: (String) -> Unit, onToken: (String) -> Unit,
             onPrompt: (String) -> Unit, onSaveSettings: () -> Unit, onClearSettings: () -> Unit,
             onChoose: () -> Unit, onRemove: () -> Unit, onGenerate: () -> Unit, onSaveImage: () -> Unit,
 onNewSession: () -> Unit, onUseTurn: (Int) -> Unit, onContinue: (Boolean) -> Unit,
 onProvider: (String) -> Unit, onRunwareKey: (String) -> Unit, onSaveRunwareKey: () -> Unit, onClearRunwareKey: () -> Unit) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showPoses by remember { mutableStateOf(false) }
    if (showPoses && !state.busy) {
        PosePicker(onDismiss = { showPoses = false }, onSelect = { pose ->
            onPrompt(PosePrompts.apply(state.prompt, pose))
            showPoses = false
        })
    }
    Column(Modifier.fillMaxSize().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("AI 图片", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { showSettings = !showSettings }) { Text("设置") }
            TextButton(onClick = onNewSession, enabled = !state.busy) { Text("新会话") }
        }
        Text("FLUX.2 Klein 4B · 输出 1024 × 1024")
        Text("云端功能需要联网。点击生成会向 Cloudflare 发送提示词和所选参考图。", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(state.provider == "cloudflare", { onProvider("cloudflare") }, label = { Text("Cloudflare") }, enabled = !state.busy)
            FilterChip(state.provider == "runware", { onProvider("runware") }, label = { Text("Runware") }, enabled = !state.busy)
        }
        if (state.provider == "runware") Text("Muse Image · 审核 low（较宽松，仍遵守平台规则）· 约 $0.01/张", style = MaterialTheme.typography.bodySmall)
        if (showSettings && state.provider == "runware") {
            OutlinedTextField(state.runwareKey, onRunwareKey, label = { Text("Runware API Key") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            Row {
                OutlinedButton(onClick = onSaveRunwareKey, enabled = !state.busy) { Text("保存 Runware Key") }
                TextButton(onClick = onClearRunwareKey, enabled = !state.busy) { Text("清除 Key") }
            }
        }
        if (showSettings && state.provider == "cloudflare") {
        OutlinedTextField(state.account, onAccount, label = { Text("Account ID") }, singleLine = true,
            enabled = !state.busy, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(state.token, onToken, label = { Text("API Token（仅保存在本机）") }, singleLine = true,
            enabled = !state.busy, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSaveSettings, enabled = !state.busy) { Text("保存设置") }
            TextButton(onClick = onClearSettings, enabled = !state.busy) { Text("清除 Token") }
        }
        Text("请保持 Cloudflare Free 套餐。用完停止，不自动重试、不自动切换付费服务；若账户已开通付费，超额计费由平台控制。", style = MaterialTheme.typography.bodySmall)
        Text("免费额度：每日 10,000 Neurons（北京时间 08:00 重置）", style = MaterialTheme.typography.bodyMedium)
        Text("实时剩余额度：暂不可读取。Cloudflare 统计可能延迟；请以官方控制台为准。", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dash.cloudflare.com/")))
        }) { Text("查看 Cloudflare 官方用量") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.turns.isEmpty()) Text("上传参考图或输入提示词开始创作")
            state.turns.forEachIndexed { index, turn ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("你：${turn.prompt}")
                        PhotoPreview(turn.image, "生成结果")
                        TextButton(onClick = { onUseTurn(index) }, enabled = !state.busy) { Text("以此图继续编辑") }
                    }
                }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
        OutlinedButton(onClick = onChoose, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.reference == null) "添加参考图（可选）" else "更换参考图")
        }
        state.reference?.let {
            PhotoPreview(it, "待上传的参考图")
            Text("上传前自动缩小至最长边 511 像素；人物长相不保证完全一致。", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRemove, enabled = !state.busy) { Text("移除参考图，改用文生图") }
        }
        if (state.result != null) {
            Row {
                Checkbox(checked = state.continueEditing, onCheckedChange = onContinue, enabled = !state.busy)
                Text("继续编辑上一张生成图", modifier = Modifier.padding(top = 12.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showPoses = true }, enabled = !state.busy) { Text("选姿势") }
        }
        OutlinedTextField(state.prompt, onPrompt, label = { Text("描述要生成或修改的图片") }, minLines = 2,
            enabled = !state.busy, modifier = Modifier.fillMaxWidth())
        Button(onClick = onGenerate, enabled = !state.busy && (if (state.provider == "runware") state.runwareKey.isNotBlank() else state.token.isNotBlank()) && state.prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) { Text(if (state.reference == null) "文生图" else "参考图生成") }
        if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在处理，请稍候…") }
        state.result?.let {
            Text("最近一次成功结果", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onSaveImage, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("保存到手机相册") }
        }
    }
}
