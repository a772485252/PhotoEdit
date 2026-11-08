package com.photoedit.app.domain

import kotlin.math.max
import kotlin.math.min

/** Platform-independent frame plan for a portrait 9:16 photo montage. */
data class MotionFrame(
    val photoIndex: Int,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val opacity: Float
)

object DressChangeMotion {
    const val FPS = 30
    const val WIDTH = 720
    const val HEIGHT = 1280

    /** Resolve the image transform at a timestamp; offsets are fractions of the cropped frame. */
    fun frameAt(timeline: DressChangeTimeline, timestampMs: Long): MotionFrame {
        require(timeline.cues.isNotEmpty())
        val time = timestampMs.coerceIn(0L, timeline.durationMs - 1)
        val cue = timeline.cues.firstOrNull { time < it.endMs } ?: timeline.cues.last()
        val position = ((time - cue.startMs).toFloat() / (cue.endMs - cue.startMs)).coerceIn(0f, 1f)
        val direction = if (cue.photoIndex % 2 == 0) 1f else -1f
        val scale = if (cue.photoIndex % 2 == 0) 1.04f + 0.10f * position else 1.14f - 0.10f * position
        val horizontal = direction * (position - 0.5f) * 0.035f
        val vertical = (position - 0.5f) * 0.018f
        val fadeWindow = min(100L, max(1L, (cue.endMs - cue.startMs) / 5)).toFloat()
        val sinceStart = (time - cue.startMs).toFloat()
        val opacity = if (cue.photoIndex == 0) 1f else (sinceStart / fadeWindow).coerceIn(0f, 1f)
        return MotionFrame(cue.photoIndex, scale, horizontal, vertical, opacity)
    }

    fun frameCount(timeline: DressChangeTimeline): Int =
        ((timeline.durationMs * FPS + 999L) / 1000L).toInt()
}
