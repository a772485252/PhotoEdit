package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class ChatModelsTest {
    @Test fun freeModeRejectsPaidOrUnverifiedCustomIds() {
        listOf("openai/gpt-5", "nvidia/nemotron-3.5-super-vl").forEach { id ->
            assertTrue(runCatching { ChatModels.validate(ChatProvider.OPENROUTER, id, false) }.isFailure)
        }
        ChatModels.validate(ChatProvider.OPENROUTER, "nvidia/nemotron-3.5-super-vl:free", false)
        ChatModels.validate(ChatProvider.OPENROUTER, "openrouter/free", false)
    }

    @Test fun paidOptInDoesNotPermitUrlsWhitespaceOrHeaderInjection() {
        listOf("", "https://example.com/model?q=x", "a model", "model\r\nheader: x", "/model").forEach { id ->
            assertTrue(runCatching { ChatModels.validate(ChatProvider.OPENROUTER, id, true) }.isFailure)
        }
        ChatModels.validate(ChatProvider.OPENROUTER, "openai/gpt-5", true)
        ChatModels.validate(ChatProvider.GROQ, "openai/gpt-oss-20b", false)
    }

    @Test fun cloudflareFreeGlmModelIsAcceptedAndRestrictedToWorkersAiIds() {
        assertEquals("cloudflare_chat_private", ChatProvider.CLOUDFLARE.namespace)
        ChatModels.validate(ChatProvider.CLOUDFLARE, "@cf/zai-org/glm-4.7-flash", false)
        assertTrue(runCatching { ChatModels.validate(ChatProvider.CLOUDFLARE, "https://example.com", false) }.isFailure)
        assertTrue(runCatching { ChatModels.validate(ChatProvider.CLOUDFLARE, "openai/gpt-oss-20b", false) }.isFailure)
    }

    @Test fun missingMalformedOrNegativePricesAreNotFree() {
        listOf(null, "", "NaN", "-1", "0.00001").forEach { assertFalse(ChatModels.zeroPrice(it)) }
        listOf("0", "0.0", "0e-6").forEach { assertTrue(ChatModels.zeroPrice(it)) }
    }

    @Test fun groqStorageIdentityIsPreservedAndProvidersAreSeparate() {
        assertEquals("groq_chat_private", ChatProvider.GROQ.namespace)
        assertNotEquals(ChatProvider.GROQ.namespace, ChatProvider.OPENROUTER.namespace)
        assertEquals("openai/gpt-oss-20b", ChatProvider.GROQ.defaultModel)
        assertTrue(ChatProvider.OPENROUTER.defaultModel.endsWith(":free"))
    }
}
