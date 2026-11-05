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

    @Test fun upgradeReplacesOnlyLegacyDefaultOnce() {
        assertEquals("Realistic Vision", HordeRules.migrateModel("stable_diffusion", false))
        assertEquals("AbsoluteReality", HordeRules.migrateModel("AbsoluteReality", false))
        assertEquals("stable_diffusion", HordeRules.migrateModel("stable_diffusion", true))
    }
    @Test fun portraitOptimizationPreservesCustomNegativePrompt() {
        val optimized = HordeRules.optimizedPrompt("woman in a blue shirt ### red clothes", "Realistic Vision")
        assertTrue(optimized.startsWith("woman in a blue shirt"))
        assertTrue(optimized.contains("red clothes"))
        assertTrue(optimized.contains("blurry face"))
        assertEquals(2, optimized.split("###").size)
        assertEquals("a landscape", HordeRules.optimizedPrompt("a landscape", "stable_diffusion"))
    }
    @Test fun customModelsDoNotInheritPortraitSampling() {
        assertEquals("k_dpmpp_2m", HordeRules.sampling("Realistic Vision").sampler)
        assertEquals("k_euler_a", HordeRules.sampling("my-custom-model").sampler)
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidReferenceStrengthCannotReachRequest() { HordeRules.denoise(Double.NaN) }
}
