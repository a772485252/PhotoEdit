package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class DressChangeBeatAudioTest {
    @Test fun producesNonSilentBoundedAudio() {
        val timeline = DressChangePlanner.plan(4)
        val audio = DressChangeBeatAudio.samples(timeline, 0, 4096)
        assertEquals(4096, audio.size)
        assertTrue(audio.any { it.toInt() != 0 })
    }

    @Test fun audioCanBeGeneratedInChunks() {
        val timeline = DressChangePlanner.plan(8)
        val first = DressChangeBeatAudio.samples(timeline, 0, 1024)
        val second = DressChangeBeatAudio.samples(timeline, 1024, 1024)
        val combined = DressChangeBeatAudio.samples(timeline, 0, 2048)
        assertArrayEquals(combined, first + second)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeAudio() {
        val timeline = DressChangePlanner.plan(2)
        DressChangeBeatAudio.samples(timeline, (timeline.durationMs * DressChangeBeatAudio.SAMPLE_RATE / 1000).toInt(), 100)
    }
}
