package com.photoedit.app.domain

object VeniceRules {
    const val GENERATE = "lustify-v8"
    const val EDIT = "firered-image-edit"
    fun model(hasReference: Boolean) = if (hasReference) EDIT else GENERATE
    fun estimate(hasReference: Boolean) = if (hasReference) "0.04" else "0.01"
    fun validateKey(key: String) {
        require(key.isNotBlank() && key.length <= 512 && key.all { it.code in 33..126 }) {
            "请填写有效的 Venice API Key，不能包含空格或换行"
        }
    }
    fun validatePrompt(prompt: String) {
        require(prompt.isNotBlank() && prompt.length <= 1500) { "Venice 本次接入支持 1 至 1500 字符，请缩短提示词后提交" }
    }
    fun validateReference(width: Int, height: Int) {
        require(width >= 256 && height >= 256 && width.toLong() * height <= 33_177_600) {
            "Venice 参考图宽高均需至少 256 像素，且不超过约 3300 万像素"
        }
    }
    // Explicit client errors reject the request. A server/transport failure can occur after inference.
    fun outcomeUnknown(http: Int) = http !in listOf(400, 401, 402, 403, 404, 413, 415, 422, 429)
    fun errorMessage(http: Int) = when (http) {
        401 -> "Venice Key 无效或没有该模型的权限，请检查设置"
        402 -> "Venice API 余额不足，请在平台核对；未自动充值"
        403 -> "Venice 拒绝访问，请检查账号资格和平台规则"
        400, 413, 415 -> "Venice 未接受请求参数或图片格式，请检查提示词与参考图"
        404 -> "Venice 模型或接口不可用；未自动更换模型"
        422 -> "Venice 拒绝了内容或图片参数，请调整输入；未自动重试"
        429 -> "Venice 请求受限或模型繁忙；未自动重试"
        else -> "Venice 响应异常（HTTP $http），结果与计费未知，请先核对平台用量"
    }
}
