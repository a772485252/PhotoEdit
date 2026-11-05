package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class ApiPodRulesTest {
    @Test fun referenceAlwaysUsesEditContractAndItsPrice() {
        assertEquals("qwen-image-edit-spicy", ApiPodRules.model(true, false))
        assertEquals("qwen-image-edit-spicy", ApiPodRules.model(true, true))
        assertEquals("0.04", ApiPodRules.estimate(true, true))
        assertEquals("z-image-spicy", ApiPodRules.model(false, false))
        assertEquals("z-image-spicy-pro", ApiPodRules.model(false, true))
    }
    @Test fun ambiguousSubmissionMustRetainOperation() {
        assertFalse(ApiPodRules.definitiveSubmissionFailure(504, ""))
        assertFalse(ApiPodRules.definitiveSubmissionFailure(409, "idempotency_in_progress"))
        assertTrue(ApiPodRules.definitiveSubmissionFailure(402, "insufficient_quota"))
        assertTrue(ApiPodRules.errorMessage(403, "").contains("地区"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun blocksCredentialHeaderInjection() { ApiPodRules.validateKey("key\r\nOther: value") }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsTaskPathTraversal() { ApiPodRules.validateTaskId("../other?key=secret") }
    @Test(expected = IllegalArgumentException::class)
    fun oversizedPromptIsRejectedWithoutTruncatingUserIntent() { ApiPodRules.validatePrompt("a".repeat(2001)) }
}
