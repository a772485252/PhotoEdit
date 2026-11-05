package com.photoedit.app.domain

data class PosePreset(val id: String, val title: String, val category: String, val description: String) {
    val prompt: String get() = "单人时尚写真，动作：$title。$description 保持自然人体比例和关节角度，四肢与手指数量正确；服装按原设计自然贴合与垂坠，呈现清楚的整体穿搭、面料和层次细节；保留原有人物身份、面部、服装及背景要求。"
}

object PoseCatalog {
    val poses = listOf(
        PosePreset("P01", "俯身回眸", "卧姿", "双膝跪在柔软床面，膝盖自然分开，上半身向前轻俯，双手在身前自然支撑，背部舒展，回头看向镜头。采用人物后侧方约45度的略高机位，轻微俯拍，画面纳入完整人物与服装轮廓，避免身体遮挡造成服装结构混乱。"),
        PosePreset("P02", "坐姿后撑", "坐姿", "正面坐在地面，双腿自然向两侧舒展，膝盖微弯，双手置于身体后方支撑，上半身轻微后仰并保持舒展。采用正前方略高于视线的机位，中远景全身构图，完整呈现人物、手脚和整套服装，避免广角变形。"),
        PosePreset("P03", "站立一字马", "站姿", "面向镜头单腿站立支撑，另一条腿向身体侧面平稳抬起并充分伸展，形成站立侧向一字马，双腿自然伸直，上半身挺直；一只手可轻扶抬起腿保持平衡，另一只手自然向侧方伸展。采用正面平视的全身镜头，留出腿部动作空间，保持水平线端正和服装轮廓完整。"),
        PosePreset("P04", "背面翘臀", "站姿", "背向镜头站立，双脚自然分开，膝盖微弯，身体重心自然，双手轻扶腰侧，头部回转看向镜头；肩部与上身轻微转向镜头，形成自然的背面三分之二角度。采用人物后侧方平视机位，完整呈现姿态与服装整体轮廓，避免过低机位和夸张透视。"),
        PosePreset("P05", "微蹲姿势", "站姿", "面向镜头站立，双脚略宽于肩，膝盖自然弯曲，身体平稳下沉，双手轻放在大腿外侧，背部保持舒展。采用正面平视或略高机位的中远景全身构图，确保头部、双手、双脚和服装完整入镜，避免近距离广角。"),
        PosePreset("P06", "椅边坐姿", "坐姿", "正面坐在椅子前沿，双膝自然分开，双脚平稳落地，双手轻放椅面两侧，上半身挺直，肩颈放松。采用正前方略偏侧约15度的平视机位，椅子不要遮挡服装主体，画面同时呈现人物坐姿和整套穿搭。"),
        PosePreset("P07", "仰卧屈膝", "卧姿", "仰卧在床面或柔软地毯上，双膝自然弯曲并舒适分开，双脚平放，双臂自然伸展于身体两侧，面部朝向镜头。采用斜上方约45度机位，而非正上方垂直俯拍，使用完整人物构图，减少透视压缩并保留服装自然褶皱与层次。"),
        PosePreset("P08", "沙发半躺", "卧姿", "半躺在沙发上，上半身由靠背自然支撑，双腿向前舒展并保持舒适间距，双手自然放在身体两侧，头部转向镜头。采用正前方略偏侧的斜上方机位，调整人物位置避免沙发扶手遮挡服装，使用中远景构图呈现完整姿态与穿搭。")
    )
}

/** Replace only the managed pose paragraph; retain the user's other prompt text. */
object PosePrompts {
    private val block = Regex("(?s)【姿势】.*?【/姿势】")
    fun apply(existing: String, pose: PosePreset): String {
        val replacement = "【姿势】${pose.prompt}【/姿势】"
        return if (block.containsMatchIn(existing)) block.replace(existing) { replacement }
        else listOf(existing.trimEnd(), replacement).filter { it.isNotBlank() }.joinToString("\\n\\n")
    }
}

/** One API request for one two-panel image. Randomness is resolved locally before submission. */
object TwoPanelPosePrompts {
    private val gridBlock = Regex("(?s)【双宫格随机姿势】.*?【/双宫格随机姿势】")
    private val singleBlock = Regex("(?s)【姿势】.*?【/姿势】")

    fun choose(pool: List<PosePreset> = PoseCatalog.poses): Pair<PosePreset, PosePreset> {
        require(pool.size >= 2) { "至少需要两个不同姿势" }
        val pair = pool.distinctBy { it.id }.shuffled().take(2)
        require(pair.size == 2) { "至少需要两个不同编号" }
        return pair[0] to pair[1]
    }

    fun apply(existing: String, left: PosePreset, right: PosePreset): String {
        require(left.id != right.id) { "左右姿势不能相同" }
        val cleaned = singleBlock.replace(existing, "").trim()
        val replacement = """
            【双宫格随机姿势】
            生成一张图片，严格左右两个等宽独立画面，两个画面各占一半；不是生成两张单独图片。
            左右为同一位成年人物，保持相同面部、发型、肤色、服装、场景和摄影光线，仅动作不同。若有参考图，以参考人物为准。
            【左画面 ${left.id}：${left.title}】${left.description}【/左画面】
            【右画面 ${right.id}：${right.title}】${right.description}【/右画面】
            两侧采用协调的时尚摄影构图，完整呈现各自动作与服装轮廓；人体比例自然，四肢和手指正确，人物不跨越分界线。
            不要第三幅画面、重复人物、文字、编号、标签、水印或多余边框。
            【/双宫格随机姿势】
        """.trimIndent()
        return if (gridBlock.containsMatchIn(cleaned)) gridBlock.replace(cleaned) { replacement }
        else listOf(cleaned, replacement).filter { it.isNotBlank() }.joinToString("\\n\\n")
    }
}
