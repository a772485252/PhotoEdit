package com.photoedit.app.domain

object ApiPodRules {
    const val STANDARD = "z-image-spicy"
    const val PRO = "z-image-spicy-pro"
    const val EDIT = "qwen-image-edit-spicy"
    fun model(hasReference: Boolean, pro: Boolean) = if (hasReference) EDIT else if (pro) PRO else STANDARD
    fun estimate(hasReference: Boolean, pro: Boolean) = if (hasReference) "0.04" else if (pro) "0.025" else "0.015"
    fun validateKey(key: String) {
        require(key.isNotBlank() && key.length <= 512 && key.all { it.code in 33..126 }) { "请填写有效的 APIPod Key，不能包含空格或换行" }
    }
    fun validatePrompt(prompt: String) {
        require(prompt.isNotBlank() && prompt.length <= 2000) { "APIPod 当前模型提示词需为 1 至 2000 字符，请缩短后提交" }
    }
    fun validateTaskId(id: String): String {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,200}"))) { "APIPod 返回了无效任务 ID；请保留原操作核对状态" }
        return id
    }
    fun errorMessage(http: Int, code: String): String = when {
        code == "insufficient_quota" || http == 402 -> "APIPod 余额或额度不足；未自动充值或切换平台"
        code == "invalid_api_key" || http == 401 -> "APIPod Key 无效或无权限，请检查设置"
        http == 403 -> "APIPod 拒绝访问，请检查账号、模型地区资格或内容规则；VPN 不保证可用"
        code == "idempotency_in_progress" -> "原提交仍在处理中，稍后继续查询；不会新建重复操作"
        code == "idempotency_conflict" -> "原操作幂等校验冲突，请先在平台核对任务"
        http == 429 -> "APIPod 请求受限，稍后继续查询原操作"
        http == 404 -> "APIPod 模型或任务不可访问，请检查平台任务记录和模型权限"
        http == 400 -> "APIPod 参数不受支持，请检查提示词及参考图要求"
        else -> "APIPod 请求异常（HTTP $http）；原操作已保留，请继续查询或在平台核对"
    }
    fun definitiveSubmissionFailure(http: Int, code: String): Boolean =
        http in listOf(400, 401, 402, 403, 404, 413, 422) && code !in listOf("idempotency_in_progress", "idempotency_conflict")
}
