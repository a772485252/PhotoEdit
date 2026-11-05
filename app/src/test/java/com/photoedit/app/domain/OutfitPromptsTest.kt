package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class OutfitPromptsTest {
    @Test fun hasDistinctStylesAndIds() {
        assertEquals(17, OutfitCatalog.presets.size)
        assertEquals(17, OutfitCatalog.presets.map { it.id }.toSet().size)
        assertEquals(setOf("国风舞蹈", "宅舞", "清纯", "时尚"), OutfitCatalog.presets.map { it.category }.toSet())
    }
    @Test fun changingOutfitKeepsOtherInstructions() {
        val original = "同一人物，瀑布背景。\n【双宫格随机姿势】左侧站立，右侧坐下。【/双宫格随机姿势】"
        val first = OutfitPrompts.apply(original, OutfitCatalog.presets[0])
        val changed = OutfitPrompts.apply(first, OutfitCatalog.presets[1])
        assertTrue(changed.contains(original))
        assertTrue(changed.contains(OutfitCatalog.presets[1].description))
        assertFalse(changed.contains(OutfitCatalog.presets[0].description))
        assertEquals(1, "【服装风格】".toRegex().findAll(changed).count())
        assertEquals(changed, OutfitPrompts.apply(changed, OutfitCatalog.presets[1]))
    }
}
