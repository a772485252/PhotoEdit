package com.photoedit.app.processing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.photoedit.app.domain.GridSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Platform adapter. All decode, crop and storage work stays off the UI thread. */
class ImageProcessor(private val context: Context) {
    private val resolver get() = context.contentResolver

    suspend fun load(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "无法读取这张图片，请选择其他图片" }
        // Bound source memory to roughly 32 MB before EXIF transformation.
        var sample = 1
        while (options.outWidth.toLong() / sample * (options.outHeight / sample) > 8_000_000L ||
            maxOf(options.outWidth, options.outHeight) / sample > 4096) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("图片解码失败")
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }

    suspend fun preview(source: Bitmap, grid: GridSpec): List<Bitmap> = withContext(Dispatchers.Default) {
        val scale = minOf(1f, 1200f / maxOf(source.width, source.height))
        val small = Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(grid.columns),
            (source.height * scale).toInt().coerceAtLeast(grid.size), true)
        try {
            grid.bounds(small.width, small.height).map { b ->
                Bitmap.createBitmap(small, b.left, b.top, b.width, b.height)
            }
        } finally { if (small !== source) small.recycle() }
    }

    suspend fun save(source: Bitmap, grid: GridSpec): Int = withContext(Dispatchers.IO) {
        val created = mutableListOf<Uri>()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + "_" + UUID.randomUUID().toString().take(8)
        try {
            grid.bounds(source.width, source.height).forEachIndexed { i, b ->
                currentCoroutineContext().ensureActive()
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoEdit_${stamp}_${(i + 1).toString().padStart(2, '0')}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoEdit")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("无法在相册创建图片")
                created.add(uri)
                val tile = Bitmap.createBitmap(source, b.left, b.top, b.width, b.height)
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        if (!tile.compress(Bitmap.CompressFormat.PNG, 100, output)) throw IOException("图片写入失败")
                    } ?: throw IOException("无法写入相册")
                } finally { tile.recycle() }
            }
            if (Build.VERSION.SDK_INT >= 29) created.forEach { uri ->
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }
            created.size
        } catch (error: Exception) {
            created.forEach { uri -> runCatching { resolver.delete(uri, null, null) } }
            throw error
        }
    }
}
