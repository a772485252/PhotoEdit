package com.photoedit.app.domain

/**
 * Compose user-written instructions, outfit and exactly one pose mode into a single
 * editable prompt. Managed blocks are replaced rather than accumulated.
 */
object PromptComposer {
    private val appearanceBlock = Regex("(?s)【人物长相】.*?【/人物长相】")
    private val outfitBlock = Regex("(?s)【服装风格】.*?【/服装风格】")
    private val poseBlock = Regex("(?s)【姿势】.*?【/姿势】")
    private val gridBlock = Regex("(?s)【双宫格随机姿势】.*?【/双宫格随机姿势】")
    private val cameraBlock = Regex("(?s)【镜头】.*?【/镜头】")

    private fun combine(existing: String, outfit: String?, pose: String?, camera: String? = cameraBlock.find(existing)?.value): String {
        val appearance = appearanceBlock.find(existing)?.value
        val custom = listOf(appearanceBlock, outfitBlock, poseBlock, gridBlock, cameraBlock).fold(existing) { text, regex ->
            regex.replace(text, "")
        }.trim()
        return listOfNotNull(custom.takeIf { it.isNotBlank() }, appearance, outfit, pose, camera)
            .joinToString("\n\n")
    }

    fun withCamera(existing: String, camera: CameraPreset): String {
        val outfit = outfitBlock.find(existing)?.value
        val pose = gridBlock.find(existing)?.value ?: poseBlock.find(existing)?.value
        val next = CameraPrompts.apply("", camera)
        return combine(existing, outfit, pose, next)
    }

    fun withAppearance(existing: String, appearance: AppearancePreset): String =
        AppearancePrompts.apply(existing, appearance)

    fun withoutAppearance(existing: String): String = AppearancePrompts.clear(existing)

    fun withOutfit(existing: String, outfit: OutfitPreset): String {
        val selectedOutfit = outfitBlock.find(existing)?.value
        val pose = gridBlock.find(existing)?.value ?: poseBlock.find(existing)?.value
        val next = OutfitPrompts.apply("", outfit)
        return combine(existing, next, pose)
    }

    fun withPose(existing: String, pose: PosePreset): String {
        val outfit = outfitBlock.find(existing)?.value
        return combine(existing, outfit, PosePrompts.apply("", pose))
    }

    fun withTwoPanel(existing: String, left: PosePreset, right: PosePreset): String {
        val outfit = outfitBlock.find(existing)?.value
        return combine(existing, outfit, TwoPanelPosePrompts.apply("", left, right))
    }
}
