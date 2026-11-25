package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class NovelRulesTest {
    @Test fun includesSavedMemoryOnceAndRetainsWholeChapter() {
        val history = (1..5).flatMap {
            listOf(ChatMessage("user", "第${it}段要求"), ChatMessage("assistant", "正文".repeat(2500)))
        }
        val memory = NovelMemory("独特的故事设定", "独特的累计前情")
        val request = NovelRules.messages(history, NovelRules.SUMMARY_REQUEST, memory)
        assertEquals(history, request.drop(1).dropLast(1))
        assertEquals(NovelRules.SUMMARY_REQUEST, request.last().text)
        assertTrue(request.first().text.contains(memory.setting))
        assertTrue(request.first().text.contains(memory.summary))
        assertEquals(1, request.count { it.role == "system" })
    }

    @Test fun discussionWorksBeforeSavingAndLongInputIsNotClipped() {
        val input = "小说要求".repeat(1000)
        val request = NovelRules.messages(emptyList(), input, NovelMemory())
        assertEquals(2, request.size)
        assertEquals(input, request.last().text)
        assertFalse(request.first().text.contains("【已保存"))
    }

    @Test fun newChapterUsesSavedMemoryWithoutOldTranscript() {
        val memory = NovelMemory("故事设定", "上一章进展")
        val request = NovelRules.messages(emptyList(), "开始下一章", memory)
        assertEquals(listOf("system", "user"), request.map { it.role })
        assertTrue(request.first().text.contains(memory.summary))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankRequest() {
        NovelRules.messages(emptyList(), " ", NovelMemory())
    }
}
