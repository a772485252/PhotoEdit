package com.photoedit.app.domain

data class PosePreset(val id: String, val title: String, val category: String, val description: String) {
    val prompt: String get() = "人物身体正面朝向镜头，头部朝向镜头，全身入镜。$description 双腿不交叉，不摆侧身姿势，保持自然的人体比例。"
}

object PoseCatalog {
    val poses = listOf(
        PosePreset("relaxed", "自然垂手", "站姿", "双脚自然分开与髋同宽，双臂放松垂在身体两侧。"),
        PosePreset("hips", "双手叉腰", "站姿", "双脚分开与肩同宽，双手轻扶腰侧，双肘向外自然展开。"),
        PosePreset("one_hip", "单手扶腰", "站姿", "双脚自然分开，左手扶腰，右臂自然下垂。"),
        PosePreset("open_arms", "双臂展开", "站姿", "双脚分开站稳，双臂向左右平举，掌心自然朝前。"),
        PosePreset("low_open", "低位展臂", "站姿", "双脚自然分开，双臂向两侧斜下方展开，肩膀放松。"),
        PosePreset("high_v", "高举双臂", "站姿", "双脚分开站立，双臂向斜上方举起，形成舒展的V字形。"),
        PosePreset("overhead", "双手上举", "站姿", "双脚自然分开，双臂伸向头顶，双手分别向上伸展，不相互交叠。"),
        PosePreset("wave", "单手挥手", "站姿", "双脚自然分开，左手举到头侧挥手，右臂自然下垂。"),
        PosePreset("both_wave", "双手招呼", "站姿", "双脚分开站稳，双肘向两侧弯曲，双手举在头部两侧，掌心朝前。"),
        PosePreset("shoulders", "双手扶肩", "站姿", "双脚自然分开，两手分别轻放同侧肩部，双肘朝外下方。"),
        PosePreset("head_sides", "双手扶头", "站姿", "双脚分开站立，双手分别轻扶头部两侧，双肘向外展开。"),
        PosePreset("hair", "单手理发", "站姿", "双脚自然分开，左手轻触同侧头发，右手自然下垂，头部保持朝向镜头。"),
        PosePreset("palms_up", "摊手展示", "站姿", "双脚自然分开，上臂贴近身体，双肘弯曲，前臂向左右展开，掌心向上。"),
        PosePreset("present", "单手展示", "站姿", "双脚自然分开，左臂屈肘向外做展示手势，右手轻扶腰侧。"),
        PosePreset("waist_front", "双手置腹前", "站姿", "双脚分开站立，双手分别放在下腹前方，手指放松，双手不交叠。"),
        PosePreset("wide", "宽距站立", "站姿", "双脚以略宽于肩的距离分开，双膝伸直但不锁死，双手轻扶腰侧。"),
        PosePreset("bend", "轻屈双膝", "站姿", "双脚分开与肩同宽，双膝轻微弯曲，双手放在同侧大腿上方，躯干正对镜头。"),
        PosePreset("reach", "单臂上伸", "站姿", "双脚分开站立，左臂向上伸展，右手轻扶腰部，胸口保持朝向镜头。"),
        PosePreset("sit_relaxed", "端坐垂手", "坐姿", "正面坐在凳子上，双膝与双脚自然分开，双脚平放地面，双臂垂在身体两侧。"),
        PosePreset("sit_thighs", "端坐扶膝", "坐姿", "正面端坐，双腿分开不交叉，双脚平放地面，双手分别轻放同侧膝盖。"),
        PosePreset("sit_hips", "端坐扶腰", "坐姿", "正面坐在凳子上，双膝自然分开，双脚平放地面，双手轻扶腰侧。"),
        PosePreset("sit_wave", "端坐挥手", "坐姿", "正面端坐，双腿自然分开，双脚落地，左手举到头侧打招呼，右手放在同侧膝盖。"),
        PosePreset("sit_open", "端坐展臂", "坐姿", "正面坐稳，双膝分开不交叉，双脚落地，双臂向左右舒展平举。"),
        PosePreset("sit_hair", "端坐理发", "坐姿", "正面端坐，双腿分开不交叉，双脚落地，左手轻触同侧头发，右手放在同侧膝盖。"),
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
