package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class AppearancePromptsTest {
    @Test fun appearanceIsOptionalAndReplaceable() {
        val preset = AppearanceCatalog.presets.first()
        val first = PromptComposer.withAppearance("瀑布背景", preset)
        assertTrue(first.contains("瀑布背景"))
        assertTrue(first.contains("【人物长相】"))
        assertEquals(first, PromptComposer.withAppearance(first, preset))
        assertEquals("瀑布背景", PromptComposer.withoutAppearance(first))
    }

    @Test fun appearanceSurvivesOutfitAndPoseChanges() {
        val initial = PromptComposer.withAppearance("自然光", AppearanceCatalog.presets.first())
        val withOutfit = PromptComposer.withOutfit(initial, OutfitCatalog.presets.first())
        val withPose = PromptComposer.withPose(withOutfit, PoseCatalog.poses.first())
        assertTrue(withPose.contains("【人物长相】"))
        assertTrue(withPose.contains("【服装风格】"))
        assertTrue(withPose.contains("【姿势】"))
        val (left, right) = TwoPanelPosePrompts.choose()
        val grid = PromptComposer.withTwoPanel(withPose, left, right)
        assertTrue(grid.contains("【人物长相】"))
        assertTrue(grid.contains("【双宫格随机姿势】"))
    }
}
