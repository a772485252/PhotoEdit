package com.photoedit.app.domain

/** Equal tiles in row-major order. size=1 means 1 row × 2 columns; size=2 means 2 × 2. */
data class TileBounds(val left: Int, val top: Int, val width: Int, val height: Int)

data class GridSpec(val size: Int) {
    init { require(size == 1 || size == 2) }
    val rows: Int get() = size
    val columns: Int get() = 2

    fun bounds(width: Int, height: Int): List<TileBounds> {
        require(width >= columns && height >= rows) { "图片尺寸太小，无法拆分" }
        val w = width / columns
        val h = height / rows
        val x = (width - w * columns) / 2
        val y = (height - h * rows) / 2
        return List(rows * columns) { i ->
            TileBounds(x + i % columns * w, y + i / columns * h, w, h)
        }
    }
}
