package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class ChatModelsTest {
    @Test fun onlyVisibleProvidersAndTheirPresetsRemain() {
        assertEquals(setOf("CLOUDFLARE", "APIPOD", "UNCENSOREDLLM"), ChatProvider.entries.map { it.name }.toSet())
        assertEquals(4, ChatProvider.entries.sumOf { ChatModels.presets(it).size })
        ChatProvider.entries.forEach { provider ->
            assertTrue(ChatModels.presets(provider).any { it.id == provider.defaultModel })
            ChatModels.presets(provider).forEach { ChatModels.validate(provider, it.id) }
        }
    }

    @Test fun activeModelsAreAcceptedByTheirOwnProviders() {
        assertEquals("cloudflare_chat_private", ChatProvider.CLOUDFLARE.namespace)
        ChatModels.validate(ChatProvider.CLOUDFLARE, "@cf/zai-org/glm-4.7-flash")
        ChatModels.validate(ChatProvider.APIPOD, "grok-4.5")
    }

    @Test fun providerModelIdsRejectUrlsWhitespaceAndHeaderInjection() {
        val invalidIds = listOf("", "https://example.com/model?q=x", "a model", "model\r\nheader: x", "/model")
        invalidIds.forEach { id ->
            assertTrue(runCatching { ChatModels.validate(ChatProvider.CLOUDFLARE, id) }.isFailure)
            assertTrue(runCatching { ChatModels.validate(ChatProvider.APIPOD, id) }.isFailure)
            assertTrue(runCatching { ChatModels.validate(ChatProvider.UNCENSOREDLLM, id) }.isFailure)
        }
    }

    @Test fun priceGuidesAreProviderAndModelSpecific() {
        val cloudflare = ChatModels.priceGuide(ChatProvider.CLOUDFLARE, "@cf/zai-org/glm-4.7-flash")
        assertNotNull(cloudflare)
        assertTrue(cloudflare!!.inputPerTenThousandChars.contains("55 Neurons"))
        assertTrue(cloudflare.outputPerTenThousandChars.contains("364 Neurons"))

        val apipod = ChatModels.priceGuide(ChatProvider.APIPOD, "grok-4.5")
        assertEquals("\$0.002–\$0.014", apipod?.inputPerTenThousandChars)
        assertEquals("\$0.006–\$0.042", apipod?.outputPerTenThousandChars)
    }

    @Test fun uncensoredLlmHasIndependentCredentialsAndModelSpecificPricing() {
        val provider = ChatProvider.UNCENSOREDLLM
        assertNotEquals(ChatProvider.APIPOD.namespace, provider.namespace)
        assertNotEquals(ChatProvider.CLOUDFLARE.namespace, provider.namespace)
        ChatModels.validate(provider, provider.defaultModel)
        assertEquals("\$0.0012", ChatModels.priceGuide(provider, provider.defaultModel)?.inputPerTenThousandChars)
        assertEquals("\$0.0042", ChatModels.priceGuide(provider, provider.defaultModel)?.outputPerTenThousandChars)
        assertNull(ChatModels.priceGuide(provider, "custom-model"))
    }

    @Test fun unknownModelDoesNotInventAPrice() {
        assertNull(ChatModels.priceGuide(ChatProvider.APIPOD, "custom-model"))
        assertNull(ChatModels.priceGuide(ChatProvider.CLOUDFLARE, "@cf/unknown/model"))
    }
}
