package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class OutfitPromptsTest {
    @Test fun hasDistinctStylesAndIds() {
        assertEquals(17, OutfitCatalog.presets.size)
        assertEquals(17, OutfitCatalog.presets.map { it.id }.toSet().size)
        assertEquals(setOf("国风舞蹈", "宅舞", "清纯", "时尚"), OutfitCatalog.presets.map { it.category }.toSet())
    }
    @Test fun explicitLowerChoicesAreDeterministic() {
        val outfit = OutfitCatalog.presets.first()
        val a = OutfitPrompts.apply("", outfit, 0)
        val b = OutfitPrompts.apply("", outfit, 1)
        assertTrue(a.contains("白色透明连裤袜"))
        assertTrue(b.contains("白色低腰蕾丝内裤"))
        assertTrue(a.contains("月白与胭脂红渐变交领短上衣"))
        assertTrue(b.contains("月白与胭脂红渐变交领短上衣"))
        assertFalse(a.contains("百褶裙"))
    }
    @Test fun changingOutfitKeepsOtherInstructions() {
        val original = "同一人物，瀑布背景。\n【双宫格随机姿势】左侧站立，右侧坐下。【/双宫格随机姿势】"
        val first = OutfitPrompts.apply(original, OutfitCatalog.presets[0])
        val changed = OutfitPrompts.apply(first, OutfitCatalog.presets[1])
        assertTrue(changed.contains(original))
        assertTrue(changed.contains("改良汉服短款对襟上衣"))
        assertFalse(changed.contains("月白与胭脂红渐变交领短上衣"))
        assertEquals(1, "【服装风格】".toRegex().findAll(changed).count())
        assertTrue(OutfitPrompts.lowerChoices.any { changed.contains(it) })
    }
}
