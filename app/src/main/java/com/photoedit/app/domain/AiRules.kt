package com.photoedit.app.domain

object AiRules {
    const val DEFAULT_ACCOUNT = "4d17f32858b5ec94a9d493d703b54fb9"
    const val MODEL = "@cf/black-forest-labs/flux-2-klein-4b"
    fun validateCredentials(account: String, token: String) {
        require(account.matches(Regex("[a-fA-F0-9]{32}"))) { "Account ID 应为 32 位十六进制字符" }
        require(token.isNotBlank() && token.length <= 512 && token.all { it.code in 33..126 }) { "请填写有效的 API Token，不能包含空格或换行" }
    }
    fun validatePrompt(prompt: String) {
        require(prompt.isNotBlank()) { "请输入提示词" }
        require(prompt.length <= 4000) { "提示词请控制在 4000 字符以内" }
    }
    fun errorMessage(http: Int, code: Int?): String = when (code) {
        3036 -> "今日免费额度已用完，请等北京时间次日早上 8 点恢复。未自动重试或切换付费服务。"
        3040 -> "模型服务繁忙，请稍后手动重试"
        5035 -> "此模型要求付费套餐，已停止。请勿为本功能自动升级套餐。"
        3007, 3008 -> "请求超时，生成结果未知；请先检查控制台用量，再决定是否重试"
        else -> when (http) {
            401, 403 -> "认证或访问被拒绝，请检查 Account ID、Token 权限及平台模型使用条款"
            429 -> "请求受限，请查看 Cloudflare 控制台；未自动重试"
            400, 413 -> "平台拒绝了请求，请检查图片、提示词及模型内容规则"
            else -> "生成失败（HTTP $http${code?.let { ", 错误码 $it" } ?: ""}），请稍后检查控制台"
        }
    }
    fun diagnosticMessage(http: Int, code: Int?, remoteMessage: String?, token: String, account: String): String {
        // Cloudflare messages are untrusted: never expose credentials or arbitrary long responses.
        val sanitized = remoteMessage.orEmpty()
            .replace(token, "[REDACTED]", ignoreCase = false)
            .replace(account, "[ACCOUNT]", ignoreCase = true)
            .replace(Regex("(?i)bearer\\s+[^\\s,;]+"), "Bearer [REDACTED]")
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .take(240)
        return buildString {
            append(errorMessage(http, code))
            append("\nHTTP: ").append(http)
            if (code != null) append(" | Cloudflare: ").append(code)
            append("\n模型: ").append(MODEL)
            if (sanitized.isNotBlank()) append("\n平台说明: ").append(sanitized)
            append("\n请勿分享 API Token。")
        }
    }

}
