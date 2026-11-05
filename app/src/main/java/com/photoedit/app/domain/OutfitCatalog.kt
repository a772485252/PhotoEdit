package com.photoedit.app.domain

data class OutfitPreset(val id: String, val title: String, val category: String, val description: String)

object OutfitCatalog {
    val presets = listOf(
        OutfitPreset("C01", "寄明月·国风舞裙", "国风舞蹈", "《寄明月》氛围的国风舞台服装：浅月白与胭脂红渐变交领短上衣，刺绣云纹，高腰轻纱百褶裙，飘逸长袖与丝带，精致腰封，轻盈舞鞋；服装层次清楚，飘带自然垂落。"),
        OutfitPreset("C02", "国风改良汉服", "国风舞蹈", "改良汉服舞蹈造型：短款对襟上衣、绣花腰封、层叠雪纺长裙、轻薄披帛、珍珠发饰，青白配色，轻盈灵动。"),
        OutfitPreset("C03", "新中式旗袍", "国风舞蹈", "新中式修身旗袍裙：立领盘扣、细腻提花面料、收腰剪裁、适度侧开衩，搭配简洁耳饰与低跟鞋，优雅现代。"),
        OutfitPreset("D01", "宅舞·甜酷偶像", "宅舞", "甜酷偶像舞台服：黑粉拼色短外套、修身内搭、高腰百褶短裙、腰带与装饰链条、及膝长袜和舞蹈短靴，层次分明。"),
        OutfitPreset("D02", "宅舞·日系学院", "宅舞", "日系学院风舞蹈装：海军领衬衫、蝴蝶结领结、格纹百褶裙、针织开衫、白色中筒袜、玛丽珍鞋，青春活泼的成年人物造型。"),
        OutfitPreset("D03", "宅舞·元气运动", "宅舞", "元气运动宅舞装：亮色短款运动夹克、合身运动背心、高腰运动短裙、运动长袜与轻量运动鞋，舒适利落，适合动态舞姿。"),
        OutfitPreset("D04", "宅舞·未来机能", "宅舞", "未来感机能舞台服：银黑拼色短夹克、结构感修身上衣、高腰工装短裙、金属扣件、功能腰带与厚底短靴，科幻灯光质感。"),
        OutfitPreset("D05", "宅舞·洛丽塔甜系", "宅舞", "甜系洛丽塔舞台裙：蕾丝方领、泡泡袖、蝴蝶结、蓬松多层裙摆、细节刺绣、白色短袜与圆头鞋，糖果色系。"),
        OutfitPreset("Q01", "清纯·白裙", "清纯", "简洁白色棉质连衣裙，方领或小圆领，收腰自然裙摆，少量蕾丝细节，平底鞋，柔和日光与自然生活感。"),
        OutfitPreset("Q02", "清纯·针织日常", "清纯", "浅色细针织开衫、纯色内搭、浅蓝高腰牛仔裙、帆布鞋，干净舒适的日常穿搭，柔和配色。"),
        OutfitPreset("Q03", "清纯·文艺森系", "清纯", "奶油色亚麻衬衫、淡绿长裙、细腰带、草编包与低跟单鞋，天然面料纹理，清新文艺氛围。"),
        OutfitPreset("Q04", "清纯·夏日海边", "清纯", "浅蓝色无袖棉麻长裙、轻薄防晒衬衫、编织凉鞋、简洁发带，清爽明亮的海边日常穿搭。"),
        OutfitPreset("F01", "时尚·都市街拍", "时尚", "剪裁利落的短西装、修身背心、高腰阔腿裤、皮质腰带、尖头短靴，都市街拍风，黑白灰配色。"),
        OutfitPreset("F02", "时尚·高级晚宴", "时尚", "缎面修身礼服长裙、精致肩线、收腰设计、简约高跟鞋和金属耳饰，优雅高级，面料反光真实。"),
        OutfitPreset("F03", "时尚·Y2K辣妹", "时尚", "Y2K时尚穿搭：亮色修身短上衣、低腰或中腰牛仔短裙、金属链条腰饰、厚底靴与彩色发夹，复古千禧年氛围。"),
        OutfitPreset("F04", "时尚·韩系简约", "时尚", "韩系简约穿搭：修身针织上衣、高腰A字半裙、轻薄西装外套、细带单鞋与极简首饰，低饱和色系。"),
        OutfitPreset("F05", "时尚·舞台亮片", "时尚", "闪亮舞台服：细密亮片修身上衣、高腰短裙、利落短靴与简约金属配饰，聚光灯下材质细节清晰。")
    )
}

object OutfitPrompts {
    private val block = Regex("(?s)【服装风格】.*?【/服装风格】")
    fun apply(existing: String, outfit: OutfitPreset): String {
        val replacement = "【服装风格】成年人物穿着：${outfit.description}。保持服装款式、颜色、材质和配饰一致，服装适配人物动作，身体结构自然。【/服装风格】"
        return if (block.containsMatchIn(existing)) block.replace(existing) { replacement }
        else listOf(existing.trimEnd(), replacement).filter { it.isNotBlank() }.joinToString("\n\n")
    }
}
