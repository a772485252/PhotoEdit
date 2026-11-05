package com.photoedit.app.domain

data class AppearancePreset(val id: String, val title: String, val description: String)

object AppearanceCatalog {
    val presets = listOf(
        AppearancePreset(
            "A001",
            "清甜知性·长发美女",
            "年轻成年东亚女性，柔和鹅蛋脸，面部轮廓精致自然，白皙细腻的肤质，清澈温柔的杏仁眼，自然舒展的眉形，秀挺鼻梁，小巧鼻尖，柔润饱满的淡粉色嘴唇，浅浅微笑。深棕黑色浓密长发，自然侧分，发丝蓬松垂落，整体清甜温柔、知性灵动，真实人像摄影质感。"
        ),
        AppearancePreset(
            "A002", "甜美元气·明亮笑容",
            "年轻成年东亚女性，柔和圆润的鹅蛋脸，饱满自然的苹果肌，明亮圆杏眼，纤长自然的睫毛，清秀小巧的鼻子，微微上扬的嘴角，甜美有感染力的笑容。深棕色蓬松长发，轻盈空气刘海，整体元气活泼、灵动可爱，真实写真质感。"
        ),
        AppearancePreset(
            "A003", "冷艳御姐·高级气质",
            "成年东亚女性，轮廓清晰的精致椭圆脸，流畅下颌线，修长微挑的眼型，深邃沉静的眼神，利落眉形，挺直鼻梁，线条分明的自然唇形，克制的浅笑。乌黑浓密长直发，干净中分，整体成熟自信、冷艳高级，写实时尚人像质感。"
        ),
        AppearancePreset(
            "A004", "纯欲氛围·温柔灵动",
            "年轻成年东亚女性，柔和精巧的小鹅蛋脸，细腻自然的肌肤，水润明亮的杏眼，柔软自然的眉形，小巧挺秀的鼻子，饱满柔润的淡玫瑰色双唇，若有若无的温柔浅笑。深棕色微卷长发，自然蓬松垂落，整体清新柔美、带有轻熟魅力，真实氛围感写真质感。"
        )
    )
}

object AppearancePrompts {
    private val block = Regex("(?s)【人物长相】.*?【/人物长相】")

    fun apply(existing: String, appearance: AppearancePreset): String {
        val replacement = "【人物长相】${appearance.description} 双眼自然直视镜头，与镜头形成清晰眼神交流，目光聚焦镜头，神态自然，不强制改变身体姿势。【/人物长相】"
        return if (block.containsMatchIn(existing)) block.replace(existing) { replacement }
        else listOf(existing.trimEnd(), replacement).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    fun clear(existing: String): String = block.replace(existing, "").trim()
}
