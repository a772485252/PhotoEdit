package com.photoedit.app.domain

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Original synthesized pop/dance percussion. No copyrighted audio samples or network calls.
 * Produces signed mono PCM16 in short chunks for a future AAC muxer.
 */
object DressChangeBeatAudio {
    const val SAMPLE_RATE = 44_100
    private const val TAU = 2.0 * PI

    fun samples(timeline: DressChangeTimeline, startSample: Int, count: Int): ShortArray {
        require(startSample >= 0 && count in 1..8192)
        val total = (timeline.durationMs * SAMPLE_RATE / 1000L).toInt()
        require(startSample.toLong() + count <= total)
        val result = ShortArray(count)
        val beats = (listOf(0L) + timeline.cues.drop(1).map { it.startMs }).distinct().sorted()
        for (i in 0 until count) {
            val sample = startSample + i
            val t = sample.toDouble() / SAMPLE_RATE
            val timeMs = sample * 1000L / SAMPLE_RATE
            val beatIndex = beats.indexOfLast { it <= timeMs }
            val beatTime = if (beatIndex >= 0) beats[beatIndex] / 1000.0 else 0.0
            val since = (t - beatTime).coerceAtLeast(0.0)
            // Punchy low kick, snare-like synthetic noise and quiet bass pulse.
            val kick = sin(TAU * (70.0 * since + 60.0 * since * since)) * exp(-since * 18.0)
            val noise = sin(TAU * 3100.0 * t) * sin(TAU * 1817.0 * t)
            val clap = if (beatIndex % 2 == 1) noise * exp(-since * 38.0) else 0.0
            val bass = sin(TAU * (110.0 + (beatIndex % 4) * 18.0) * t) * 0.08
            val fadeIn = (t / 0.08).coerceIn(0.0, 1.0)
            val fadeOut = ((timeline.durationMs / 1000.0 - t) / 0.16).coerceIn(0.0, 1.0)
            val mixed = (kick * 0.55 + clap * 0.16 + bass) * fadeIn * fadeOut
            result[i] = (mixed.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
        }
        return result
    }
}
