package com.photoedit.app.domain

/** APIPod can report model-routing failures as HTTP 503 rather than 404. */
object ChatErrors {
    fun apipodRejection(status: Int, code: String, message: String): String? {
        val detail = "$code $message"
        return when {
            detail.contains("insufficient_user_quota", ignoreCase = true) ->
                "APIPod 返回额度不足（HTTP $status）。请核对当前 Key 对应账户、独立额度或消费上限；不会自动重试。"
            code.equals("model_not_available", ignoreCase = true) ||
                detail.contains("model_not_found", ignoreCase = true) ||
                message.contains("No available request for model", ignoreCase = true) ||
                message.contains("no available channel", ignoreCase = true) ->
                "APIPod 当前 Key 可访问的分组中没有该模型的可用通道（HTTP $status）。请在平台控制台核对模型 ID、Key 的分组权限和通道状态，或在设置中手动更换模型。输入已保留；未自动重试或切换模型。"
            else -> null
        }
    }
}
