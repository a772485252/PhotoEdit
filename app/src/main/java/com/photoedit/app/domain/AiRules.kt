package com.photoedit.app.domain

object AiRules {
    const val DEFAULT_ACCOUNT = "4d17f32858b5ec94a9d493d703b54fb9"
    fun validateCredentials(account: String, token: String) {
        require(account.matches(Regex("[a-fA-F0-9]{32}"))) { "Account ID 应为 32 位十六进制字符" }
        require(token.isNotBlank() && token.length <= 512 && token.all { it.code in 33..126 }) { "请填写有效的 API Token，不能包含空格或换行" }
    }
}
