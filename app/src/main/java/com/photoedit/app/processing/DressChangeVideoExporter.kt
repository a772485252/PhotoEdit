package com.photoedit.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.photoedit.app.domain.DressChangeMotion
import com.photoedit.app.domain.DressChangeTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import java.io.File

/**
 * Offline silent MP4 encoder. Uses byte-buffer YUV420 for broad Android API 26 compatibility.
 * Caller owns the resulting cache file; no gallery write occurs here.
 */
class DressChangeVideoExporter(private val context: Context) {
    suspend fun export(
        photos: List<Uri>,
        timeline: DressChangeTimeline,
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.Default) {
        require(photos.size == timeline.cues.size && photos.size >= 2)
        val width = DressChangeMotion.WIDTH
        val height = DressChangeMotion.HEIGHT
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val supported = codec.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
            .colorFormats.toList()
        val color = when {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar in supported ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar in supported ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            else -> {
                codec.release()
                error("设备不支持所需的 YUV420 视频编码格式")
            }
        }
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, color)
            setInteger(MediaFormat.KEY_BIT_RATE, 3_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, DressChangeMotion.FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val output = File(context.cacheDir, "dress_change_${System.currentTimeMillis()}.mp4")
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var track = -1
        var currentBitmap: Bitmap? = null
        var currentIndex = -1
        val pixels = IntArray(width * height)
        val yuv = ByteArray(width * height * 3 / 2)
        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(frameBitmap)
        val info = MediaCodec.BufferInfo()
        var started = false
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            started = true
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val frames = DressChangeMotion.frameCount(timeline)
            var submitted = 0
            var eosSent = false
            var finished = false
            while (!finished) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                if (!eosSent) {
                    val index = codec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        if (submitted < frames) {
                            val timeMs = submitted * 1000L / DressChangeMotion.FPS
                            val motion = DressChangeMotion.frameAt(timeline, timeMs)
                            if (currentIndex != motion.photoIndex) {
                                currentBitmap?.recycle()
                                currentBitmap = decodeSampled(photos[motion.photoIndex])
                                currentIndex = motion.photoIndex
                            }
                            DressChangeFrameRenderer.draw(canvas, requireNotNull(currentBitmap), motion, width, height)
                            frameBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                            rgbToYuv420(pixels, yuv, width, height, color)
                            val buffer = requireNotNull(codec.getInputBuffer(index))
                            buffer.clear()
                            buffer.put(yuv)
                            codec.queueInputBuffer(index, 0, yuv.size, submitted * 1_000_000L / DressChangeMotion.FPS, 0)
                            submitted++
                            if (submitted % 15 == 0 || submitted == frames) onProgress(submitted * 100 / frames)
                        } else {
                            codec.queueInputBuffer(index, 0, 0, frames * 1_000_000L / DressChangeMotion.FPS, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eosSent = true
                        }
                    }
                }
                var draining = true
                while (draining) {
                    val index = codec.dequeueOutputBuffer(info, 10_000)
                    when (index) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> draining = false
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted)
                            track = requireNotNull(muxer).addTrack(codec.outputFormat)
                            requireNotNull(muxer).start()
                            muxerStarted = true
                        }
                        else -> if (index >= 0) {
                            if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                check(muxerStarted)
                                val buffer = requireNotNull(codec.getOutputBuffer(index))
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                requireNotNull(muxer).writeSampleData(track, buffer, info)
                            }
                            finished = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(index, false)
                            if (finished) draining = false
                        }
                    }
                }
            }
            check(muxerStarted)
            requireNotNull(muxer).stop()
            muxerStarted = false
            check(output.length() > 0L) { "视频编码结果为空" }
            output
        } catch (error: Throwable) {
            throw error
        } finally {
            currentBitmap?.recycle()
            frameBitmap.recycle()
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { if (started) codec.stop() } catch (_: Exception) {}
            codec.release()
            if (!output.exists() || output.length() == 0L) output.delete()
        }
    }

    private fun decodeSampled(uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "无法读取照片" }
        var sample = 1
        while (options.outWidth / sample > 1600 || options.outHeight / sample > 1600) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        return requireNotNull(bitmap) { "照片解码失败" }
    }

    private fun rgbToYuv420(argb: IntArray, out: ByteArray, width: Int, height: Int, format: Int) {
        val planar = format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        val area = width * height
        for (y in 0 until height) for (x in 0 until width) {
            val pixel = argb[y * width + x]
            val r = pixel shr 16 and 255
            val g = pixel shr 8 and 255
            val b = pixel and 255
            val luma = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
            out[y * width + x] = luma.coerceIn(0, 255).toByte()
            if (x % 2 == 0 && y % 2 == 0) {
                val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255).toByte()
                val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255).toByte()
                val offset = y / 2 * (width / 2) + x / 2
                if (planar) {
                    out[area + offset] = u
                    out[area + area / 4 + offset] = v
                } else {
                    out[area + offset * 2] = u
                    out[area + offset * 2 + 1] = v
                }
            }
        }
    }
}
