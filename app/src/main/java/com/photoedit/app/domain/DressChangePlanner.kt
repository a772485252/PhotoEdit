package com.photoedit.app.domain

/**
 * Offline dress-change montage timeline. All timings are in milliseconds.
 * Music assets must be original or appropriately licensed before bundling.
 */
data class BeatTemplate(
    val id: String,
    val durationMs: Long,
    val beatsMs: List<Long>,
    val musicAsset: String? = null
) {
    init {
        require(durationMs in 1_000L..60_000L)
        require(beatsMs.isNotEmpty() && beatsMs.all { it in 1 until durationMs })
        require(beatsMs == beatsMs.distinct().sorted())
    }
}

data class PhotoCue(val photoIndex: Int, val startMs: Long, val endMs: Long) {
    init { require(photoIndex >= 0 && startMs >= 0 && endMs > startMs) }
}

data class DressChangeTimeline(
    val templateId: String,
    val durationMs: Long,
    val cues: List<PhotoCue>
)

/** Deterministic beat-aligned cuts; never invents or downloads music. */
object DressChangePlanner {
    val templates = listOf(
        BeatTemplate("pop_8s", 8_000, listOf(2_000, 3_000, 4_000, 5_000, 6_000, 7_000)),
        BeatTemplate("pop_12s", 12_000, listOf(2_000, 3_500, 5_000, 6_500, 8_000, 9_500, 11_000)),
        BeatTemplate("pop_15s", 15_000, listOf(2_500, 4_000, 5_500, 7_000, 8_500, 10_000, 11_500, 13_000))
    )

    fun plan(photoCount: Int, template: BeatTemplate = choose(photoCount)): DressChangeTimeline {
        require(photoCount in 2..30) { "请选择 2～30 张照片" }
        val cuts = if (photoCount - 1 <= template.beatsMs.size) {
            (1 until photoCount).map { index ->
                template.beatsMs[((index.toLong() * template.beatsMs.size) / photoCount).toInt()]
            }
        } else {
            // More photos than beats: divide beat intervals while preserving exact duration.
            (1 until photoCount).map { index ->
                template.durationMs * index / photoCount
            }
        }
        val boundaries = listOf(0L) + cuts + template.durationMs
        val cues = (0 until photoCount).map { index ->
            PhotoCue(index, boundaries[index], boundaries[index + 1])
        }
        return DressChangeTimeline(template.id, template.durationMs, cues)
    }

    fun choose(photoCount: Int): BeatTemplate {
        require(photoCount in 2..30)
        return when {
            photoCount <= 5 -> templates[0]
            photoCount <= 9 -> templates[1]
            else -> templates[2]
        }
    }
}
