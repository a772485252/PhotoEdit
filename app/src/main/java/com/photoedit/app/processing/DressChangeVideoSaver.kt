package com.photoedit.app.processing

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Only copies a finished cache video to the public gallery after an explicit save tap. */
class DressChangeVideoSaver(private val context: Context) {
    suspend fun save(file: File): Uri = withContext(Dispatchers.IO) {
        require(file.isFile && file.length() > 0) { "视频文件不存在" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "PhotoEdit_${UUID.randomUUID()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PhotoEdit")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("无法创建相册视频")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("无法写入视频")
            if (Build.VERSION.SDK_INT >= 29) {
                resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }, null, null)
            }
            uri
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }
}
