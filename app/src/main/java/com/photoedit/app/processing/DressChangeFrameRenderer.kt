package com.photoedit.app.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.photoedit.app.domain.DressChangeMotion
import com.photoedit.app.domain.MotionFrame
import kotlin.math.max

/** Draws one portrait frame into an existing Canvas; never allocates full-frame bitmaps. */
object DressChangeFrameRenderer {
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, source: Bitmap, frame: MotionFrame, width: Int, height: Int) {
        require(width > 0 && height > 0)
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        canvas.drawColor(Color.BLACK)
        val fit = max(width.toFloat() / source.width, height.toFloat() / source.height)
        val drawnWidth = source.width * fit * frame.scale
        val drawnHeight = source.height * fit * frame.scale
        val x = (width - drawnWidth) / 2f + frame.offsetX * width
        val y = (height - drawnHeight) / 2f + frame.offsetY * height
        imagePaint.alpha = (frame.opacity.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawBitmap(source, null, RectF(x, y, x + drawnWidth, y + drawnHeight), imagePaint)
        imagePaint.alpha = 255
        // A short, subtle white flash at each new look's first frame.
        overlayPaint.color = Color.WHITE
        overlayPaint.alpha = ((1f - frame.opacity.coerceIn(0f, 1f)) * 90).toInt()
        if (overlayPaint.alpha > 0) canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }
}
