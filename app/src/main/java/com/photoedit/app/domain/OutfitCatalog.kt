package com.photoedit.app.domain

data class OutfitPreset(val id: String, val title: String, val category: String, val description: String)

object OutfitCatalog {
    val presets = listOf(
        OutfitPreset("C01", "国风·飘带舞姬", "国风舞蹈", "月白与樱粉拼色短款舞蹈上衣，轻盈宽袖、飘带与细致刺绣，搭配高开衩轻纱短裙，灵动甜美"),
        OutfitPreset("C02", "新中式·高开衩舞裙", "国风舞蹈", "浅绯色短款盘扣上衣，柔软丝缎与轻透薄纱叠搭，高开衩飘逸舞裙，轻快俏皮"),
        OutfitPreset("D01", "宅舞·软萌偶像", "宅舞", "粉白配色宽松短款露肩上衣，蓬松泡泡袖、细丝带和小蝴蝶结，轻盈舞台造型"),
        OutfitPreset("D02", "宅舞·甜酷猫系", "宅舞", "黑粉拼色宽松短款连帽上衣，斜肩设计、轻薄网纱拼接和猫耳装饰，甜酷俏皮"),
        OutfitPreset("D03", "宅舞·元气运动", "宅舞", "蜜桃色宽松短款运动上衣，落肩短袖、抽绳与轻薄叠穿细节，明亮元气"),
        OutfitPreset("Q01", "甜美·草莓奶油", "甜美", "奶白色轻盈露肩短上衣，荷叶边、细蕾丝和草莓粉蝴蝶结，柔软浪漫"),
        OutfitPreset("Q02", "可爱·糖果泡泡袖", "甜美", "浅蓝色短款泡泡袖上衣，方领、轻薄雪纺叠层和细小珍珠装饰，甜美活泼"),
        OutfitPreset("F01", "时尚·甜辣闪耀", "时尚", "香槟粉不对称短款上衣，轻柔垂坠领口、微闪薄纱与细肩带层次，俏皮舞台时尚")
    )
}

/** Replaces only the managed clothing block; selection stays concise and editable. */
object OutfitPrompts {
    private val block = Regex("(?s)【服装风格】.*?【/服装风格】")
    val lowerChoices = listOf(
        "白色低腰蕾丝内搭，作为原服装的内层细节自然可见",
        "白色连裤袜，作为原服装搭配的内层单品清晰呈现"
    )

    fun apply(existing: String, outfit: OutfitPreset, lowerIndex: Int = lowerChoices.indices.random()): String {
        require(lowerIndex in lowerChoices.indices)
        val replacement = "【服装风格】成年人物，完整保留所选服饰原有的款式、上衣、下装、颜色、材质与剪裁，不替换、不删减、不重新设计原服装；${outfit.description}。内搭细节：${lowerChoices[lowerIndex]}，在原服装自然形成的层次与边缘处适度可见，呈现清晰但含蓄的时尚穿搭效果。【/服装风格】"
        return if (block.containsMatchIn(existing)) block.replace(existing) { replacement }
        else listOf(existing.trimEnd(), replacement).filter { it.isNotBlank() }.joinToString("\n\n")
    }
}
