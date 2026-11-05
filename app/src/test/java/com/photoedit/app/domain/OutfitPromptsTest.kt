package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class OutfitPromptsTest {
    @Test fun hasEightDistinctStylesAndIds() {
        assertEquals(8, OutfitCatalog.presets.size)
        assertEquals(8, OutfitCatalog.presets.map { it.id }.toSet().size)
        assertEquals(setOf("国风舞蹈", "宅舞", "甜美", "时尚"), OutfitCatalog.presets.map { it.category }.toSet())
    }

    @Test fun explicitInnerLayerChoicesAreDeterministic() {
        val outfit = OutfitCatalog.presets.first()
        val lace = OutfitPrompts.apply("", outfit, 0)
        val tights = OutfitPrompts.apply("", outfit, 1)
        assertTrue(lace.contains("白色蕾丝装饰内衬"))
        assertTrue(tights.contains("白色连裤袜"))
        assertTrue(lace.contains("月白与樱粉拼色短款舞蹈上衣"))
        assertTrue(tights.contains("月白与樱粉拼色短款舞蹈上衣"))
        assertTrue(lace.contains("高开衩半透明轻纱外裙"))
        assertFalse(lace.contains("替换原服装"))
    }

    @Test fun changingOutfitKeepsOtherInstructionsAndReplacesOutfitBlock() {
        val original = "同一人物，瀑布背景。\n【双宫格随机姿势】左侧站立，右侧坐下。【/双宫格随机姿势】"
        val first = OutfitPrompts.apply(original, OutfitCatalog.presets[0], 0)
        val changed = OutfitPrompts.apply(first, OutfitCatalog.presets[1], 1)
        assertTrue(changed.contains("同一人物，瀑布背景。"))
        assertTrue(changed.contains("【双宫格随机姿势】左侧站立，右侧坐下。【/双宫格随机姿势】"))
        assertTrue(changed.contains("浅绯色短款盘扣上衣"))
        assertFalse(changed.contains("月白与樱粉拼色短款舞蹈上衣"))
        assertEquals(1, "【服装风格】".toRegex().findAll(changed).count())
        assertTrue(changed.contains("白色连裤袜"))
    }
}
