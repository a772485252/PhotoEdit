package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class PromptComposerTest {
    @Test fun outfitThenPoseKeepsBothAndUserInstructions() {
        val outfit = OutfitCatalog.presets.first()
        val pose = PoseCatalog.poses.first()
        val a = PromptComposer.withOutfit("瀑布背景，保持人物面部。", outfit)
        val result = PromptComposer.withPose(a, pose)
        assertTrue(result.contains("瀑布背景，保持人物面部。"))
        assertTrue(result.contains(outfit.description))
        assertTrue(result.contains(pose.description))
        assertEquals(1, "【服装风格】".toRegex().findAll(result).count())
        assertEquals(1, "【姿势】".toRegex().findAll(result).count())
    }

    @Test fun poseThenOutfitKeepsBothAndReplacesOutfit() {
        val pose = PoseCatalog.poses.first()
        val first = PromptComposer.withOutfit(PromptComposer.withPose("摄影棚", pose), OutfitCatalog.presets[0])
        val changed = PromptComposer.withOutfit(first, OutfitCatalog.presets[1])
        assertTrue(changed.contains("摄影棚"))
        assertTrue(changed.contains(pose.description))
        assertTrue(changed.contains(OutfitCatalog.presets[1].description))
        assertFalse(changed.contains(OutfitCatalog.presets[0].description))
        assertEquals(1, "【服装风格】".toRegex().findAll(changed).count())
    }

    @Test fun switchingSingleAndTwoPanelDoesNotAccumulatePoseBlocks() {
        val first = PromptComposer.withPose("保持场景", PoseCatalog.poses[0])
        val grid = PromptComposer.withTwoPanel(first, PoseCatalog.poses[1], PoseCatalog.poses[2])
        assertFalse(grid.contains("【姿势】"))
        assertTrue(grid.contains("【双宫格随机姿势】"))
        val single = PromptComposer.withPose(grid, PoseCatalog.poses[3])
        assertFalse(single.contains("【双宫格随机姿势】"))
        assertEquals(1, "【姿势】".toRegex().findAll(single).count())
        assertTrue(single.contains("保持场景"))
    }
}
