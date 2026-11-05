package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class PosePromptsTest {
    @Test fun switchingPoseKeepsOtherPromptAndReplacesOldPose() {
        val original = "保持原人物和服装，背景为瀑布。"
        val first = PosePrompts.apply(original, PoseCatalog.poses[0])
        val switched = PosePrompts.apply(first, PoseCatalog.poses[1])
        assertTrue(switched.startsWith(original))
        assertFalse(switched.contains(PoseCatalog.poses[0].description))
        assertTrue(switched.contains(PoseCatalog.poses[1].description))
        assertEquals(switched, PosePrompts.apply(switched, PoseCatalog.poses[1]))
    }
    @Test fun selectedAtlasIdsAreUnique() {
        assertEquals(29, PoseCatalog.poses.size)
        assertEquals(29, PoseCatalog.poses.map { it.id }.toSet().size)
        assertEquals("P001", PoseCatalog.poses.first().id)
        assertEquals("P080", PoseCatalog.poses.last().id)
        assertTrue(PoseCatalog.poses.all { it.description.isNotBlank() })
    }
    @Test fun emptyPromptAcceptsPose() {
        val pose = PoseCatalog.poses.first()
        assertEquals("【姿势】${pose.prompt}【/姿势】", PosePrompts.apply("", pose))
    }
}
