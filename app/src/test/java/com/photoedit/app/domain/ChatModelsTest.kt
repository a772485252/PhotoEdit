package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class ChatModelsTest {
    @Test fun onlyVisibleProvidersAndTheirPresetsRemain() {
        assertEquals(setOf("CLOUDFLARE", "APIPOD", "UNCENSOREDLLM"), ChatProvider.entries.map { it.name }.toSet())
        assertEquals(8, ChatProvider.entries.sumOf { ChatModels.presets(it).size })
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
        assertEquals("uncensoredllm_chat_private", provider.namespace)
        assertEquals("glm-5.3-uncensored", provider.defaultModel)
        assertEquals(listOf("glm-5.3-uncensored", "glm-5.3-abliterated", "deepseek-v4-pro-uncensored",
            "gemma-4-31b-queen", "glm-5.3-flash-abliterated"), ChatModels.presets(provider).map { it.id })
        ChatModels.validate(provider, provider.defaultModel)
        assertEquals("\$0.0032", ChatModels.priceGuide(provider, provider.defaultModel)?.inputPerTenThousandChars)
        assertEquals("\$0.0097", ChatModels.priceGuide(provider, provider.defaultModel)?.outputPerTenThousandChars)
        assertNull(ChatModels.priceGuide(provider, "glm-5.3-flash-uncensored"))
        assertNull(ChatModels.priceGuide(provider, "custom-model"))
    }

    @Test fun restoredModelsMigrateReplacedPresetsAndPreserveOtherSelections() {
        val provider = ChatProvider.UNCENSOREDLLM
        assertEquals(provider.defaultModel, ChatModels.restoreModel(provider, "glm-5.3-flash-uncensored"))
        assertEquals(provider.defaultModel, ChatModels.restoreModel(provider, provider.defaultModel))
        assertEquals("custom-model", ChatModels.restoreModel(provider, "custom-model"))
        ChatModels.presets(provider).forEach { preset ->
            assertEquals(preset.id, ChatModels.restoreModel(provider, preset.id))
        }
        assertEquals(ChatProvider.APIPOD.defaultModel, ChatModels.restoreModel(ChatProvider.APIPOD, "deepseek-flash"))
        assertEquals("grok-4.6", ChatModels.restoreModel(ChatProvider.APIPOD, "grok-4.6"))
        assertEquals("glm-5.3-flash-uncensored", ChatModels.restoreModel(ChatProvider.APIPOD, "glm-5.3-flash-uncensored"))
        assertEquals(ChatProvider.CLOUDFLARE.defaultModel,
            ChatModels.restoreModel(ChatProvider.CLOUDFLARE, ChatProvider.CLOUDFLARE.defaultModel))
    }

    @Test fun unknownModelDoesNotInventAPrice() {
        assertNull(ChatModels.priceGuide(ChatProvider.APIPOD, "custom-model"))
        assertNull(ChatModels.priceGuide(ChatProvider.CLOUDFLARE, "@cf/unknown/model"))
    }

    @Test fun uncensoredComparisonModelsUseTheirOwnPricesWithoutLeakingToOtherProviders() {
        val expected = mapOf(
            "glm-5.3-abliterated" to ("\$0.0233" to "\$0.0466"),
            "deepseek-v4-pro-uncensored" to ("\$0.0094" to "\$0.0188"),
            "gemma-4-31b-queen" to ("\$0.0107" to "\$0.0107"),
            "glm-5.3-flash-abliterated" to ("\$0.0047" to "\$0.0194")
        )
        expected.forEach { (id, prices) ->
            val guide = ChatModels.priceGuide(ChatProvider.UNCENSOREDLLM, id)
            assertEquals(prices.first, guide?.inputPerTenThousandChars)
            assertEquals(prices.second, guide?.outputPerTenThousandChars)
            assertNull(ChatModels.priceGuide(ChatProvider.APIPOD, id))
            assertNull(ChatModels.priceGuide(ChatProvider.CLOUDFLARE, id))
        }
    }
}
