package com.photoedit.app.domain

/** Portable geometry: equal tiles, centered crop of at most n-1 pixels per axis. */
data class TileBounds(val left: Int, val top: Int, val width: Int, val height: Int)

data class GridSpec(val size: Int) {
    init { require(size in 2..4) }
    fun bounds(width: Int, height: Int): List<TileBounds> {
        require(width >= size && height >= size) { "图片尺寸太小，无法拆分" }
        val w = width / size
        val h = height / size
        val x = (width - w * size) / 2
        val y = (height - h * size) / 2
        return List(size * size) { i -> TileBounds(x + i % size * w, y + i / size * h, w, h) }
    }
}
