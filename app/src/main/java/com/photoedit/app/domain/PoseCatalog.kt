package com.photoedit.app.domain

data class PosePreset(val id: String, val title: String, val category: String, val description: String) {
    val prompt: String get() = "单人全身摄影，动作：$title。$description 保持自然人体比例和关节角度，四肢与手指数量正确，保留原有人物身份、面部、服装及背景要求。"
}

object PoseCatalog {
    val poses = listOf(
        PosePreset("P001", "S形站姿·胯部侧顶", "站姿", "重心放在一侧腿上，骨盆向侧面推出，腰线形成S形，肩膀向反方向轻倾。"),
        PosePreset("P002", "背部后仰·双手撩发", "站姿", "一脚在前一脚在后，腰部轻轻扭转，骨盆朝镜头斜侧方，胸口打开，上身适度后仰，双手抬至头发两侧，双肘展开。采用全身三分之二侧面机位，清晰呈现腰线、胯部与腿部衔接。"),
        PosePreset("P003", "顶胯回眸", "站姿", "身体以背面斜对镜头，骨盆向侧方移，头部越过肩膀回望镜头。"),
        PosePreset("P004", "前倾·手扶膝", "站姿", "髋部折叠，上身向前倾，双手分别扶住膝盖，背部自然伸展。"),
        PosePreset("P006", "手扶墙顶胯（站立后入待位）", "站姿", "侧身靠近墙面，双掌在肩高处轻扶墙壁，上身微微前倾，骨盆向后上方自然推出，后背形成柔和曲线。镜头位于人物斜后侧，清晰呈现腰臀和胯部外轮廓，双腿完整可见。"),
        PosePreset("P007", "背向回望·强调臀部", "站姿", "背对镜头呈三分之二侧身站姿，重心压在一侧腿上，骨盆向镜头一侧推出，腰部反向扭转，头部越肩回望。双臂自然置于腰侧，不遮挡腰臀和胯部曲线，镜头完整拍摄全身。"),
        PosePreset("P010", "站立挤胸", "站姿", "身体以三分之二侧面朝向镜头，一腿向前轻迈，骨盆微微侧转，腰臀形成流畅曲线；双臂自然交叠于胸前，肩部回转看向镜头。镜头取腰部至大腿清晰可见的全身构图，不遮挡胯部轮廓。"),
        PosePreset("P012", "站立V字（单腿侧高抬）", "站姿", "单腿站稳，另一条腿向侧上方高抬成V形，双臂展开维持平衡。"),
        PosePreset("P014", "站立前屈翘臀·双手触地", "站姿", "双脚站立，髋部大幅前屈，双手向下触地，膝盖自然微弯。"),
        PosePreset("P015", "双手扶腰·单膝内收后仰", "站姿", "双手轻扶腰线，重心放在后腿，前腿屈膝点地，骨盆向侧前方倾斜，上半身轻微后仰，头部回望镜头。以三分之二侧身角度展现腰臀和胯部曲线，手臂不遮挡髋部。"),
        PosePreset("P016", "侧坐·单手支撑·身体柔弯", "坐姿", "侧坐在地，一手在身体侧后方撑地，双腿弯曲偏向同侧，腰部柔和侧弯。"),
        PosePreset("P017", "正坐·挺胸后仰", "坐姿", "双膝跪坐，胸口向上打开，上身略后仰，双臂自然放松。"),
        PosePreset("P018", "M字开腿（坐位·双手后撑）", "坐姿", "坐在地面，双手在身后撑地，两膝弯曲并向两侧打开，双脚落地。"),
        PosePreset("P019", "M字开腿（坐位·双手扶膝）", "坐姿", "坐在地面，双膝弯曲向左右打开，双手分别扶膝，躯干挺直。"),
        PosePreset("P020", "大幅V字开腿·双手后撑", "坐姿", "坐在地面，双手后撑，两条腿向左右大幅伸展开成V形。"),
        PosePreset("P022", "M字开腿（双肘支撑后仰）", "坐姿", "坐在地面，双膝弯曲向两侧打开，双肘向后撑地，上身后仰。"),
        PosePreset("P027", "鸭子坐·双手前撑挤胸", "坐姿", "双膝弯曲跪坐，双腿向身体两侧折叠，双手撑在身前，上身前倾。"),
        PosePreset("P029", "坐姿·单腿大幅外开（单M字）", "坐姿", "坐在地面，一腿弯曲靠近身体，另一腿向侧方伸展，单手撑地。"),
        PosePreset("P031", "跪立·上体后仰·双手抱后脑", "跪姿", "双膝跪地，上身后仰，双手放在后脑附近，双肘向外打开。"),
        PosePreset("P032", "四肢着地·挺背目视前方", "跪姿", "双手和双膝撑地，背部略向下舒展，头部抬起看向前方。"),
        PosePreset("P033", "双膝大幅外开的跪立·压胯下沉", "跪姿", "双膝向两侧分开跪地，骨盆下沉，躯干直立，双手自然放在大腿附近。"),
        PosePreset("P047", "俯卧托腮·双腿上抬", "卧姿", "俯卧，双肘撑地，双手托腮，双膝弯曲，小腿向后上方抬起。"),
        PosePreset("P054", "俯卧翘臀", "卧姿", "俯卧，前臂放在身前，骨盆略抬起，膝盖自然弯曲。"),
        PosePreset("P055", "仰卧V字开腿（双腿抬起）", "卧姿", "仰卧，双腿抬起向两侧分开形成V形，双臂放在身体两侧。"),
        PosePreset("P060", "俯卧蛙腿（双膝外展）", "卧姿", "俯卧，双膝弯曲向外打开，小腿向后，前臂放在身前。"),
        PosePreset("P066", "桥式高抬胯·双膝打开", "特殊", "仰卧，双脚踩地，膝盖弯曲向两侧打开，骨盆向上抬起成桥式。"),
        PosePreset("P070", "蹲姿开腿（混混蹲）", "特殊", "双脚分开低位蹲下，膝盖向两侧打开，双肘自然放在膝盖附近。"),
        PosePreset("P078", "蹲姿开腿·双手抱头后", "特殊", "宽距下蹲，双膝朝外，双手扶后脑，双肘向左右展开。"),
        PosePreset("P080", "反向四肢着地（桌面式）·向上顶胯", "特殊", "面朝上，以双手和双脚撑地，骨盆抬起呈反向桌面，胸口朝上。"),
        PosePreset("X001", "俯身趴伏·回眸抬臀", "卧姿", "成年人物面朝下俯身趴伏在柔软平面，前臂在身体前方自然支撑，上胸略抬，双膝弯曲着地，骨盆与臀部向上抬起，腰背形成平滑自然的曲线。头部向一侧转动，越过肩膀回望镜头。镜头从人物斜后侧略高位置拍摄，完整呈现头部、背部、臀部与双腿，腰臀和胯部外轮廓清晰，不被手臂或道具遮挡，保持自然关节角度。"),
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
