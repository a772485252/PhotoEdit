package com.photoedit.app.domain

/**
 * Compose user-written instructions, outfit and exactly one pose mode into a single
 * editable prompt. Managed blocks are replaced rather than accumulated.
 */
object PromptComposer {
    private val outfitBlock = Regex("(?s)【服装风格】.*?【/服装风格】")
    private val poseBlock = Regex("(?s)【姿势】.*?【/姿势】")
    private val gridBlock = Regex("(?s)【双宫格随机姿势】.*?【/双宫格随机姿势】")

    private fun combine(existing: String, outfit: String?, pose: String?): String {
        val custom = listOf(outfitBlock, poseBlock, gridBlock).fold(existing) { text, regex ->
            regex.replace(text, "")
        }.trim()
        return listOfNotNull(custom.takeIf { it.isNotBlank() }, outfit, pose)
            .joinToString("\n\n")
    }

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
