package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class VeniceRulesTest {
    @Test fun referenceSwitchesBothEndpointModelAndDisplayedPrice() {
        assertEquals("z-image-turbo", VeniceRules.model(false))
        assertEquals("0.01", VeniceRules.estimate(false))
        assertEquals("firered-image-edit", VeniceRules.model(true))
        assertEquals("0.04", VeniceRules.estimate(true))
    }
    @Test fun explicitRejectionDoesNotLeaveAmbiguousBillingLock() {
        listOf(400, 401, 402, 403, 404, 413, 415, 422, 429).forEach { assertFalse(VeniceRules.outcomeUnknown(it)) }
        listOf(301, 408, 500, 502, 503, 504).forEach { assertTrue(VeniceRules.outcomeUnknown(it)) }
    }
    @Test fun keyRejectsHeaderInjectionWithoutEchoingSecret() {
        val key = "private-key\r\nX-Test: injected"
        val error = runCatching { VeniceRules.validateKey(key) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertFalse(error!!.message.orEmpty().contains("private-key"))
    }
    @Test fun referenceLimitsIncludeMinimumShortEdgeAndLongOverflow() {
        VeniceRules.validateReference(256, 1024)
        VeniceRules.validateReference(1024, 1024)
        listOf(255 to 1024, 1024 to 255, Int.MAX_VALUE to Int.MAX_VALUE).forEach { (w, h) ->
            assertTrue(runCatching { VeniceRules.validateReference(w, h) }.isFailure)
        }
    }
    @Test fun promptTooLongIsRejectedRatherThanSilentlyTruncated() {
        VeniceRules.validatePrompt("a".repeat(1500))
        assertTrue(runCatching { VeniceRules.validatePrompt("a".repeat(1501)) }.isFailure)
        assertTrue(runCatching { VeniceRules.validatePrompt("  ") }.isFailure)
    }
}
