package com.photoedit.app.processing

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.photoedit.app.domain.DressChangeBeatAudio
import com.photoedit.app.domain.DressChangeTimeline
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Encodes original offline PCM beats to AAC and remuxes with the finished AVC MP4. */
object DressChangeAudioMuxer {
    suspend fun addOriginalBeat(silentVideo: File, timeline: DressChangeTimeline): File {
        require(silentVideo.isFile && silentVideo.length() > 0)
        val audio = File(silentVideo.parentFile, silentVideo.nameWithoutExtension + "_beat.m4a")
        val combined = File(silentVideo.parentFile, silentVideo.nameWithoutExtension + "_audio.mp4")
        try {
            encodeAudio(audio, timeline)
            remux(silentVideo, audio, combined)
            check(combined.length() > 0L) { "音画合成失败" }
            return combined
        } catch (error: Throwable) {
            combined.delete()
            throw error
        } finally {
            audio.delete()
        }
    }

    private suspend fun encodeAudio(output: File, timeline: DressChangeTimeline) {
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        var muxer: MediaMuxer? = null
        var started = false
        var muxStarted = false
        try {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, DressChangeBeatAudio.SAMPLE_RATE, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            started = true
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val totalSamples = (timeline.durationMs * DressChangeBeatAudio.SAMPLE_RATE / 1000L).toInt()
            var sent = 0
            var eos = false
            var done = false
            var track = -1
            val info = MediaCodec.BufferInfo()
            while (!done) {
                currentCoroutineContext().ensureActive()
                if (!eos) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = requireNotNull(codec.getInputBuffer(inputIndex))
                        input.clear()
                        val count = minOf(4096, input.remaining() / 2, totalSamples - sent)
                        if (count > 0) {
                            input.order(ByteOrder.LITTLE_ENDIAN)
                            for (value in DressChangeBeatAudio.samples(timeline, sent, count)) input.putShort(value)
                            codec.queueInputBuffer(inputIndex, 0, count * 2, sent * 1_000_000L / DressChangeBeatAudio.SAMPLE_RATE, 0)
                            sent += count
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, 0, sent * 1_000_000L / DressChangeBeatAudio.SAMPLE_RATE, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eos = true
                        }
                    }
                }
                var draining = true
                while (draining) {
                    val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
                    when (outputIndex) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> draining = false
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxStarted)
                            track = requireNotNull(muxer).addTrack(codec.outputFormat)
                            requireNotNull(muxer).start()
                            muxStarted = true
                        }
                        else -> if (outputIndex >= 0) {
                            if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                check(muxStarted)
                                val data = requireNotNull(codec.getOutputBuffer(outputIndex))
                                data.position(info.offset)
                                data.limit(info.offset + info.size)
                                requireNotNull(muxer).writeSampleData(track, data, info)
                            }
                            done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                            if (done) draining = false
                        }
                    }
                }
            }
        } finally {
            try { if (muxStarted) muxer?.stop() } finally {
                muxer?.release()
                if (started) codec.stop()
                codec.release()
            }
        }
    }

    private suspend fun remux(video: File, audio: File, output: File) {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        try {
            videoExtractor.setDataSource(video.absolutePath)
            audioExtractor.setDataSource(audio.absolutePath)
            val videoIndex = (0 until videoExtractor.trackCount).first {
                videoExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            }
            val audioIndex = (0 until audioExtractor.trackCount).first {
                audioExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            videoExtractor.selectTrack(videoIndex)
            audioExtractor.selectTrack(audioIndex)
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoTrack = muxer.addTrack(videoExtractor.getTrackFormat(videoIndex))
            val audioTrack = muxer.addTrack(audioExtractor.getTrackFormat(audioIndex))
            muxer.start()
            started = true
            copyTrack(videoExtractor, muxer, videoTrack)
            copyTrack(audioExtractor, muxer, audioTrack)
        } finally {
            try { if (started) muxer?.stop() } finally {
                muxer?.release()
                videoExtractor.release()
                audioExtractor.release()
            }
        }
    }

    private suspend fun copyTrack(extractor: MediaExtractor, muxer: MediaMuxer, track: Int) {
        val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024)
        val info = MediaCodec.BufferInfo()
        while (true) {
            currentCoroutineContext().ensureActive()
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            require(size <= buffer.capacity()) { "视频样本过大" }
            val flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                MediaCodec.BUFFER_FLAG_KEY_FRAME
            } else {
                0
            }
            info.set(0, size, extractor.sampleTime, flags)
            buffer.position(0)
            buffer.limit(size)
            muxer.writeSampleData(track, buffer, info)
            extractor.advance()
        }
    }
}
