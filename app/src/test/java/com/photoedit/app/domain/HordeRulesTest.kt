package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class HordeRulesTest {
    @Test fun blankKeyUsesAnonymousWithoutOtherPlatformCredentials() {
        assertEquals("0000000000", HordeRules.apiKey("  "))
        assertEquals("personal-key", HordeRules.apiKey(" personal-key "))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsHeaderInjection() { HordeRules.apiKey("valid\r\napikey: stolen") }
    @Test fun queueMessagesDistinguishGeneratingFromNoEligibleWorker() {
        assertEquals("AI Horde 正在生成…", HordeRules.queueMessage(1, 123, 300, true))
        assertTrue(HordeRules.queueMessage(0, 123, 300, false).contains("暂无匹配节点"))
        val queued = HordeRules.queueMessage(0, 123, 300, true)
        assertTrue(queued.contains("123"))
        assertTrue(queued.contains("非承诺"))
    }
}
