package com.photoedit.app.domain

data class OutfitPreset(val id: String, val title: String, val category: String, val description: String)

object OutfitCatalog {
    val presets = listOf(
        OutfitPreset("C01", "寄明月·灵动短衫", "国风舞蹈", "月白与胭脂红拼色短款交领舞蹈上衣，露出自然腰线，轻薄飘带和云纹刺绣，灵动俏皮"),
        OutfitPreset("C02", "国风·俏皮抹胸叠穿", "国风舞蹈", "青白配色抹胸式舞蹈上衣，外搭短款薄纱披肩，精致刺绣和细丝带，轻盈性感"),
        OutfitPreset("C03", "新中式·修身短上衣", "国风舞蹈", "改良盘扣短款修身上衣，利落方领和微露肩设计，丝缎光泽与东方纹样，现代活泼"),
        OutfitPreset("D01", "宅舞·甜酷偶像", "宅舞", "黑粉拼色露腰短夹克搭配贴身短背心，金属链条点缀，甜酷舞台感"),
        OutfitPreset("D02", "宅舞·俏皮学院风", "宅舞", "短款海军领系带上衣，修身剪裁和小蝴蝶结，活泼俏皮的成年宅舞舞台造型"),
        OutfitPreset("D03", "宅舞·元气运动", "宅舞", "亮色短款运动夹克搭配运动抹胸背心，露出自然腰线，元气有活力"),
        OutfitPreset("D04", "宅舞·未来机能", "宅舞", "银黑拼色露肩机能短上衣，贴身剪裁、金属扣件和不对称肩带，未来感舞台造型"),
        OutfitPreset("D05", "宅舞·甜辣蝴蝶结", "宅舞", "粉白拼色修身短上衣，心形领口、细肩带、迷你蝴蝶结和少量蕾丝，甜辣俏皮"),
        OutfitPreset("Q01", "清新·甜美吊带", "清纯", "奶白色方领细肩带短上衣，小荷叶边和精致蕾丝，清爽甜美"),
        OutfitPreset("Q02", "清新·短款针织", "清纯", "浅粉色贴身短款针织上衣，宽领口、短袖和精致纽扣，俏皮自然"),
        OutfitPreset("Q03", "清新·碎花露肩", "清纯", "淡绿色碎花一字肩短上衣，轻薄荷叶边和收腰剪裁，明快浪漫"),
        OutfitPreset("Q04", "清新·海边吊带", "清纯", "浅蓝色细肩带短款背心，轻盈棉质纹理和清爽配色，阳光活泼"),
        OutfitPreset("F01", "时尚·辣妹街拍", "时尚", "黑色修身挂脖短上衣，利落露肩设计和金属饰环，都市甜酷街拍风"),
        OutfitPreset("F02", "时尚·缎面抹胸", "时尚", "香槟色缎面抹胸式上衣，立体剪裁与柔亮光泽，性感时尚"),
        OutfitPreset("F03", "时尚·Y2K辣妹", "时尚", "亮色贴身短上衣，不对称领口和复古金属小配饰，Y2K千禧辣妹风"),
        OutfitPreset("F04", "时尚·韩系甜辣", "时尚", "浅灰色修身短款针织上衣，露肩领口和细致罗纹，韩系甜辣风"),
        OutfitPreset("F05", "时尚·亮片舞台", "时尚", "银色亮片细肩带短上衣，贴身剪裁和闪亮舞台质感，动感吸睛")
    )
}

/** Selected upperwear is vivid and modern; lowerwear is randomized locally. */
object OutfitPrompts {
    private val block = Regex("(?s)【服装风格】.*?【/服装风格】")
    val lowerChoices = listOf(
        "白色透明连裤袜",
        "白色低腰蕾丝内裤"
    )

    fun apply(existing: String, outfit: OutfitPreset, lowerIndex: Int = lowerChoices.indices.random()): String {
        require(lowerIndex in lowerChoices.indices)
        val upper = outfit.description
        val replacement = "【服装风格】成年人物；上衣：${upper}；下装：${lowerChoices[lowerIndex]}。【/服装风格】"
        return if (block.containsMatchIn(existing)) block.replace(existing) { replacement }
        else listOf(existing.trimEnd(), replacement).filter { it.isNotBlank() }.joinToString("\n\n")
    }
}
