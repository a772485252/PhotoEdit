package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class ChatErrorsTest {
    @Test fun modelRoutingCodeTakesPrecedenceOver503RetryAdvice() {
        val result = ChatErrors.apipodRejection(503, "model_not_found", "Unavailable")!!
        assertTrue(result.contains("可用通道"))
        assertTrue(result.contains("HTTP 503"))
        assertFalse(result.contains("稍后手动重试"))
        assertFalse(result.contains("Unavailable"))
    }

    @Test fun routingMessageWorksWithoutMachineCode() {
        assertNotNull(ChatErrors.apipodRejection(503, "", "No available request for model example-model"))
        assertNotNull(ChatErrors.apipodRejection(503, "", "No available channel"))
        assertNotNull(ChatErrors.apipodRejection(404, "model_not_available", "Unavailable"))
    }

    @Test fun ordinaryServerFailureKeepsGenericHandling() {
        assertNull(ChatErrors.apipodRejection(503, "upstream_error", "Service unavailable"))
        assertNull(ChatErrors.apipodRejection(401, "invalid_api_key", "Invalid key"))
    }

    @Test fun quotaFailureKeepsItsOwnGuidance() {
        val result = ChatErrors.apipodRejection(403, "insufficient_user_quota", "Quota exhausted")!!
        assertTrue(result.contains("额度不足"))
        assertFalse(result.contains("可用通道"))
    }
}
