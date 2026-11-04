package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class AiRulesTest {
    @Test fun defaultAccountAndNormalTokenAreValid() { AiRules.validateCredentials(AiRules.DEFAULT_ACCOUNT, "test-token") }
    @Test(expected = IllegalArgumentException::class) fun rejectsAccountPathInjection() { AiRules.validateCredentials("../other", "test-token") }
    @Test(expected = IllegalArgumentException::class) fun rejectsHeaderInjection() { AiRules.validateCredentials(AiRules.DEFAULT_ACCOUNT, "test\r\nHeader:value") }
    @Test(expected = IllegalArgumentException::class) fun rejectsBlankPrompt() { AiRules.validatePrompt("   ") }
    @Test fun distinguishesQuotaFromBusyAndAuthentication() {
        assertTrue(AiRules.errorMessage(429, 3036).contains("免费额度"))
        assertTrue(AiRules.errorMessage(429, 3040).contains("繁忙"))
        assertTrue(AiRules.errorMessage(401, null).contains("认证"))
        assertTrue(AiRules.errorMessage(403, 5035).contains("付费套餐"))
    }
}
