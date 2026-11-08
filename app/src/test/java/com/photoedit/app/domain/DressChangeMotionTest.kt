package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class DressChangeMotionTest {
    @Test fun framePlanCoversWholeVideo() {
        val timeline = DressChangePlanner.plan(8)
        assertEquals(360, DressChangeMotion.frameCount(timeline))
        assertEquals(0, DressChangeMotion.frameAt(timeline, 0).photoIndex)
        assertEquals(7, DressChangeMotion.frameAt(timeline, timeline.durationMs).photoIndex)
    }

    @Test fun beatCutSwitchesPhoto() {
        val timeline = DressChangePlanner.plan(4)
        val firstCut = timeline.cues[0].endMs
        assertEquals(0, DressChangeMotion.frameAt(timeline, firstCut - 1).photoIndex)
        assertEquals(1, DressChangeMotion.frameAt(timeline, firstCut).photoIndex)
    }

    @Test fun frameTransformIsBounded() {
        val timeline = DressChangePlanner.plan(30)
        for (i in 0 until DressChangeMotion.frameCount(timeline)) {
            val frame = DressChangeMotion.frameAt(timeline, i * 1000L / DressChangeMotion.FPS)
            assertTrue(frame.scale in 1.0f..1.2f)
            assertTrue(frame.opacity in 0f..1f)
            assertTrue(frame.offsetX in -0.05f..0.05f)
        }
    }
}
