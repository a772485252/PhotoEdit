package com.photoedit.app.domain

import org.junit.Test

class AiRulesTest {
    @Test fun defaultAccountAndNormalTokenAreValid() { AiRules.validateCredentials(AiRules.DEFAULT_ACCOUNT, "test-token") }
    @Test(expected = IllegalArgumentException::class) fun rejectsAccountPathInjection() { AiRules.validateCredentials("../other", "test-token") }
    @Test(expected = IllegalArgumentException::class) fun rejectsHeaderInjection() { AiRules.validateCredentials(AiRules.DEFAULT_ACCOUNT, "test\r\nHeader:value") }

}
