package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class GridSpecTest {
    @Test fun equalTilesInRowMajorOrderForEveryPreset() {
        for (n in 2..4) {
            val tiles = GridSpec(n).bounds(1003, 757)
            assertEquals(n * n, tiles.size)
            tiles.forEachIndexed { i, b ->
                assertEquals(1003 / n, b.width)
                assertEquals(757 / n, b.height)
                assertEquals((1003 % n) / 2 + i % n * b.width, b.left)
                assertEquals((757 % n) / 2 + i / n * b.height, b.top)
                assertTrue(b.left + b.width <= 1003)
                assertTrue(b.top + b.height <= 757)
            }
        }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsTooSmallImage() { GridSpec(4).bounds(3, 10) }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedGrid() { GridSpec(5) }
}
