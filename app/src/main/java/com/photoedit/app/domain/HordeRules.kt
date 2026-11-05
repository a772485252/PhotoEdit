package com.photoedit.app.domain

data class HordeModel(val name: String, val workers: Int)

object HordeRules {
    const val ANONYMOUS_KEY = "0000000000"
    fun apiKey(value: String): String {
        val key = value.trim().ifBlank { ANONYMOUS_KEY }
        require(key.length <= 512 && key.all { it.code in 33..126 }) { "AI Horde Key 不能包含空格或换行" }
        return key
    }
    fun validateModel(value: String): String {
        val model = value.trim()
        require(model.isNotEmpty() && model.length <= 200 && model.none { it.isISOControl() }) { "请选择或填写 AI Horde 模型" }
        return model
    }
    fun queueMessage(processing: Int, queue: Int, seconds: Int, possible: Boolean): String = when {
        processing > 0 -> "AI Horde 正在生成…"
        !possible -> "AI Horde 暂无匹配节点，仍在等待；可停止等待，稍后继续查询。"
        else -> buildString {
            append("AI Horde 排队中")
            if (queue >= 0) append(" · 全局队列位置 ").append(queue)
            if (seconds >= 0) append(" · 平台估计约 ").append(seconds).append(" 秒（非承诺）")
        }
    }
}
