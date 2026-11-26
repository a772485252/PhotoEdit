package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class ChatRulesTest {
    @Test fun retainsEntireHistoryAndLongInputInBothModes() {
        val history = (1..10).flatMap { listOf(ChatMessage("user", "u".repeat(1500)), ChatMessage("assistant", "a".repeat(1500))) }
        val input = "new request".repeat(2000)
        listOf(true, false).forEach { promptMode ->
            val request = ChatRules.messages(history, input, promptMode)
            assertEquals("system", request.first().role)
            assertEquals(input, request.last().text)
            assertEquals(history, request.drop(1).dropLast(1))
            assertEquals(history.size + 2, request.size)
        }
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
    @Test fun customRulesTakeEffectWithoutDroppingConversationContext() {
        val history = listOf(ChatMessage("user", "之前的提示词"), ChatMessage("assistant", "之前的扩写结果"))
        val request = ChatRules.messages(history, "继续修改", true, "新的自定义扩写规则")
        assertEquals("新的自定义扩写规则", request.first().text)
        assertEquals("之前的提示词", request[1].text)
        assertEquals("之前的扩写结果", request[2].text)
        assertEquals("继续修改", request.last().text)
    }

    @Test fun longCustomRulesAreNotClipped() {
        val rules = "完整扩写要求".repeat(3000)
        val request = ChatRules.messages(emptyList(), "继续", true, rules)
        assertEquals(rules, request.first().text)
    }

    @Test(expected = IllegalArgumentException::class) fun blankRequestRejected() { ChatRules.messages(emptyList(), " ", true) }
}
