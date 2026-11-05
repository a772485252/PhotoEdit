package com.photoedit.app.domain

data class AppearancePreset(val id: String, val title: String, val description: String)

object AppearanceCatalog {
    val presets = listOf(
        AppearancePreset(
            "A001",
            "清甜知性·长发美女",
            "年轻成年东亚女性，柔和鹅蛋脸，面部轮廓精致自然，白皙细腻的肤质，清澈温柔的杏仁眼，自然舒展的眉形，秀挺鼻梁，小巧鼻尖，柔润饱满的淡粉色嘴唇，浅浅微笑。深棕黑色浓密长发，自然侧分，发丝蓬松垂落，整体清甜温柔、知性灵动，真实人像摄影质感。"
        )
    )
}

object AppearancePrompts {
    private val block = Regex("(?s)【人物长相】.*?【/人物长相】")

    fun apply(existing: String, appearance: AppearancePreset): String {
        val replacement = "【人物长相】${appearance.description}【/人物长相】"
        return if (block.containsMatchIn(existing)) block.replace(existing) { replacement }
        else listOf(existing.trimEnd(), replacement).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    fun clear(existing: String): String = block.replace(existing, "").trim()
}
