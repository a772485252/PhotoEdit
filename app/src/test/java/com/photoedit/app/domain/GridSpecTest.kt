package com.photoedit.app.domain

import org.junit.Assert.*
import org.junit.Test

class GridSpecTest {
    @Test fun supportsOnlyOneByTwoAndTwoByTwo() {
        for (rows in 1..2) {
            val spec = GridSpec(rows)
            val tiles = spec.bounds(1003, 757)
            assertEquals(rows * 2, tiles.size)
            tiles.forEachIndexed { i, b ->
                assertEquals(1003 / 2, b.width)
                assertEquals(757 / rows, b.height)
                assertEquals((1003 % 2) / 2 + i % 2 * b.width, b.left)
                assertEquals((757 % rows) / 2 + i / 2 * b.height, b.top)
                assertTrue(b.left + b.width <= 1003)
                assertTrue(b.top + b.height <= 757)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTooNarrowImage() { GridSpec(1).bounds(1, 10) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedGrid() { GridSpec(3) }
}
