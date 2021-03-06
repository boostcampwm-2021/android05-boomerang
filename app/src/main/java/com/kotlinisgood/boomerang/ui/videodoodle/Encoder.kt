package com.kotlinisgood.boomerang.ui.videodoodle

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.*
import java.io.File

class Encoder(
    width: Int,
    height: Int,
    outputVideo: File?
) {
    val inputSurface: Surface
    private var encoder: MediaCodec
    private val bufferInfo: MediaCodec.BufferInfo
    private lateinit var encodedFormat: MediaFormat
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val mediaMuxer =
        MediaMuxer(outputVideo!!.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var videoTrack = -1
    private var isMuxerStart = false

    init {
        val format = MediaFormat.createVideoFormat("video/avc", width, height)

        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)

        encoder = MediaCodec.createEncoderByType("video/avc")
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        encoder.start()

        bufferInfo = MediaCodec.BufferInfo()
    }

    fun transferBuffer() {
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            while (true) {
                when (val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> break
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        encodedFormat = encoder.outputFormat
                        videoTrack = mediaMuxer.addTrack(encodedFormat)
                        mediaMuxer.start()
                        isMuxerStart = true
                    }
                    else -> {
                        val encoderOutputBuffers = encoder.getOutputBuffer(encoderStatus)
                            ?: throw Exception("MediaCodec.getOutputBuffer is null")
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) bufferInfo.size =
                            0
                        if (bufferInfo.size != 0) {
                            if (isMuxerStart) mediaMuxer.writeSampleData(
                                videoTrack,
                                encoderOutputBuffers,
                                bufferInfo
                            )
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
        }
    }

    fun stopEncoder() {
        isMuxerStart = false
        scope.cancel()
        mediaMuxer.stop()
        mediaMuxer.release()
    }

    companion object {
        private const val BIT_RATE = 6000000
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 5
    }
}