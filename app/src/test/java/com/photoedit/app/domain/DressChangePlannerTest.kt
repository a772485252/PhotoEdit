package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class DressChangePlannerTest {
    @Test fun templatesHaveOrderedBeats() {
        DressChangePlanner.templates.forEach { template ->
            assertTrue(template.beatsMs.zipWithNext().all { (a, b) -> a < b })
            assertTrue(template.beatsMs.last() < template.durationMs)
        }
    }

    @Test fun shortMontageCutsOnBeats() {
        val template = DressChangePlanner.templates[0]
        val timeline = DressChangePlanner.plan(5, template)
        assertEquals(5, timeline.cues.size)
        assertEquals(0L, timeline.cues.first().startMs)
        assertEquals(8_000L, timeline.cues.last().endMs)
        assertTrue(timeline.cues.dropLast(1).all { it.endMs in template.beatsMs })
        assertTrue(timeline.cues.zipWithNext().all { (a, b) -> a.endMs == b.startMs })
    }

    @Test fun thirtyPhotosHavePositiveDuration() {
        val timeline = DressChangePlanner.plan(30)
        assertEquals(30, timeline.cues.size)
        assertTrue(timeline.cues.all { it.endMs > it.startMs })
        assertEquals(15_000L, timeline.cues.last().endMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSinglePhoto() { DressChangePlanner.plan(1) }
}
