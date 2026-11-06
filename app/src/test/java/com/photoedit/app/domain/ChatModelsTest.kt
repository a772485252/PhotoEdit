package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class ChatModelsTest {
    @Test fun cloudflareAndVeniceModelsAreAcceptedByTheirOwnProviders() {
        assertEquals("cloudflare_chat_private", ChatProvider.CLOUDFLARE.namespace)
        assertEquals("venice_chat_private", ChatProvider.VENICE.namespace)
        ChatModels.validate(ChatProvider.CLOUDFLARE, "@cf/zai-org/glm-4.7-flash", false)
        ChatModels.validate(ChatProvider.VENICE, "venice-uncensored-1-2", false)
    }

    @Test fun providerModelIdsRejectUrlsWhitespaceAndHeaderInjection() {
        val invalidIds = listOf("", "https://example.com/model?q=x", "a model", "model\r\nheader: x", "/model")
        invalidIds.forEach { id ->
            assertTrue(runCatching { ChatModels.validate(ChatProvider.CLOUDFLARE, id, false) }.isFailure)
            assertTrue(runCatching { ChatModels.validate(ChatProvider.VENICE, id, false) }.isFailure)
        }
    }

    @Test fun missingMalformedOrNegativePricesAreNotFree() {
        listOf(null, "", "NaN", "-1", "0.00001").forEach { assertFalse(ChatModels.zeroPrice(it)) }
        listOf("0", "0.0", "0e-6").forEach { assertTrue(ChatModels.zeroPrice(it)) }
    }
}
