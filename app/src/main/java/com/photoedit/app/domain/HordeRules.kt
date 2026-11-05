package com.photoedit.app.domain

data class HordeModel(val name: String, val workers: Int)

data class HordeSampling(val sampler: String, val steps: Int, val cfg: Double, val karras: Boolean)

object HordeRules {
    const val DEFAULT_MODEL = "Realistic Vision"
    val portraitModels = listOf("Realistic Vision", "AbsoluteReality")
    fun migrateModel(saved: String?, migrated: Boolean): String =
        if (!migrated && (saved.isNullOrBlank() || saved == "stable_diffusion")) DEFAULT_MODEL
        else saved?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
    fun sampling(model: String): HordeSampling =
        if (model in portraitModels) HordeSampling("k_dpmpp_2m", 28, 6.0, true)
        else HordeSampling("k_euler_a", 25, 7.0, false)
    fun denoise(value: Double): Double {
        require(value.isFinite() && value in 0.2..0.7) { "参考图修改强度应在 0.2 至 0.7 之间" }
        return value
    }
    fun optimizedPrompt(prompt: String, model: String): String {
        if (model !in portraitModels) return prompt
        val parts = prompt.split("###", limit = 2)
        val positive = parts[0].trimEnd() + ", photograph, natural skin texture, detailed eyes, sharp focus"
        val negative = listOf(parts.getOrNull(1)?.trim().orEmpty(),
            "blurry face, low quality, deformed face, distorted eyes, plastic skin, oversmoothed skin")
            .filter { it.isNotBlank() }.joinToString(", ")
        return "$positive ### $negative"
    }

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
