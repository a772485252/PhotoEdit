package com.photoedit.app.domain

data class PosePreset(val id: String, val title: String, val category: String, val description: String) {
    val prompt: String get() = "单人全身摄影，动作：$title。$description 保持自然人体比例和关节角度，四肢与手指数量正确，保留原有人物身份、面部、服装及背景要求。"
}

object PoseCatalog {
    val poses = listOf(
        PosePreset("P01", "俯身回眸", "卧姿", "双膝跪在床面，膝盖自然分开，上半身向前俯低，双手向前支撑，腰部自然弯曲，臀部抬高，回头望向镜头"),
        PosePreset("P02", "坐姿后撑", "坐姿", "正面坐在地面，双腿向左右打开，双膝自然微弯，双手置于身体后方撑地，上半身略微后仰"),
        PosePreset("P03", "站立一字马", "站姿", "面向镜头，单腿直立支撑身体，另一条腿向身体侧面高高抬起，伸展至接近180度，形成站立一字马姿势，双腿保持伸直，上半身挺直，一只手扶住抬起的脚踝保持平衡，另一只手自然向侧方伸展"),
        PosePreset("P04", "背面翘臀", "站姿", "背对镜头站立，双脚自然分开，双膝微弯，腰部轻微前倾，臀部自然向后突出，双手扶在腰部两侧，头部回转看向镜头"),
        PosePreset("P05", "微蹲姿势", "站姿", "面向镜头，双脚分开略宽于肩，双膝向外弯曲，身体缓缓下沉，双手轻扶大腿外侧"),
        PosePreset("P06", "椅边坐姿", "坐姿", "正面坐在椅子前沿，双膝自然分开，双脚平放地面，双手扶住座椅两侧，上半身挺直"),
        PosePreset("P07", "仰卧屈膝", "卧姿", "仰卧在床面，双膝向上弯曲并自然分开，双脚平放床面，双臂向身体两侧伸展，面部朝向镜头"),
        PosePreset("P08", "沙发半躺", "卧姿", "半躺在沙发上，上半身靠住靠背，双腿向前伸展并自然分开，双手放在身体两侧，头部转向镜头")
    )
}

/** Replace only the managed pose paragraph; retain the user's other prompt text. */
object PosePrompts {
    private val block = Regex("(?s)【姿势】.*?【/姿势】")
    fun apply(existing: String, pose: PosePreset): String {
        val replacement = "【姿势】${pose.prompt}【/姿势】"
        return if (block.containsMatchIn(existing)) block.replace(existing) { replacement }
        else listOf(existing.trimEnd(), replacement).filter { it.isNotBlank() }.joinToString("\n\n")
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
            两侧分别完整呈现动作，人体比例自然，四肢和手指正确，人物不跨越分界线。
            不要第三幅画面、重复人物、文字、编号、标签、水印或多余边框。
            【/双宫格随机姿势】
        """.trimIndent()
        return if (gridBlock.containsMatchIn(cleaned)) gridBlock.replace(cleaned) { replacement }
        else listOf(cleaned, replacement).filter { it.isNotBlank() }.joinToString("\n\n")
    }
}
