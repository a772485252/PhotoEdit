package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class ChatRulesTest {
    @Test fun keepsLatestRequestAndCompleteRecentTurnsUnderBudget() {
        val history = (1..10).flatMap { listOf(ChatMessage("user", "u".repeat(1500)), ChatMessage("assistant", "a".repeat(1500))) }
        val request = ChatRules.messages(history, "new request", true)
        assertEquals("system", request.first().role)
        assertEquals("new request", request.last().text)
        assertTrue(request.drop(1).sumOf { it.text.length } <= 6000)
        assertEquals(listOf("system", "user", "assistant", "user"), request.map { it.role })
    }
    @Test fun emptyHistoryCreatesOnlySystemAndCurrentUser() {
        assertEquals(2, ChatRules.messages(emptyList(), "写个瀑布写真提示词", true).size)
    }
    @Test fun promptModeUsesUnambiguousAnatomyGuidance() {
        val system = ChatRules.messages(emptyList(), "写真", true).first().text
        assertTrue(system.contains("明确且唯一"))
        assertTrue(system.contains("缺失"))
    }
    @Test fun closeFullBodyInstructionIsPresent() {
        val text = ChatRules.messages(emptyList(), "portrait", true).first().text
        assertTrue(text.contains("近距离全身构图"))
        assertTrue(text.contains("不输出方案列表"))
    }
    @Test fun modeChoosesDifferentInstructions() {
        assertNotEquals(ChatRules.messages(emptyList(), "hello", true).first(), ChatRules.messages(emptyList(), "hello", false).first())
    }
    @Test(expected = IllegalArgumentException::class) fun blankRequestRejected() { ChatRules.messages(emptyList(), " ", true) }
}
