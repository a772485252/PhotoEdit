package com.photoedit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.photoedit.app.ui.components.PhotoPreview
import com.photoedit.app.viewmodel.AiUiState

@Composable
fun AiScreen(state: AiUiState, onAccount: (String) -> Unit, onToken: (String) -> Unit,
             onPrompt: (String) -> Unit, onSaveSettings: () -> Unit, onClearSettings: () -> Unit,
             onChoose: () -> Unit, onRemove: () -> Unit, onGenerate: () -> Unit, onSaveImage: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("AI 图片 · Cloudflare", style = MaterialTheme.typography.headlineSmall)
        Text("FLUX.2 Klein 4B · 输出 1024 × 1024")
        Text("云端功能需要联网。点击生成会向 Cloudflare 发送提示词和所选参考图。", style = MaterialTheme.typography.bodySmall)
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
        OutlinedButton(onClick = onChoose, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.reference == null) "添加参考图（可选）" else "更换参考图")
        }
        state.reference?.let {
            PhotoPreview(it, "待上传的参考图")
            Text("上传前自动缩小至最长边 511 像素；人物长相不保证完全一致。", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRemove, enabled = !state.busy) { Text("移除参考图，改用文生图") }
        }
        OutlinedTextField(state.prompt, onPrompt, label = { Text("提示词") }, minLines = 3,
            enabled = !state.busy, modifier = Modifier.fillMaxWidth())
        Button(onClick = onGenerate, enabled = !state.busy && state.token.isNotBlank() && state.prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) { Text(if (state.reference == null) "文生图" else "参考图生成") }
        if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在处理，请稍候…") }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.result?.let {
            Text("最近一次成功结果", style = MaterialTheme.typography.titleLarge)
            PhotoPreview(it, "AI 生成结果")
            Button(onClick = onSaveImage, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("保存到手机相册") }
        }
    }
}
