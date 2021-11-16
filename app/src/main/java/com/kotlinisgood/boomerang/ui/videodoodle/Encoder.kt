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
    bitrate: Int,
    frameRate: Int,
    desiredSpanSec: Int,
) {
    val inputSurface: Surface
    private var encoder: MediaCodec
    private val bufferInfo: MediaCodec.BufferInfo
    private var encodedFormat: MediaFormat? = null
    val encoderBuffer: CircularEncoderBuffer =
        CircularEncoderBuffer(bitrate, frameRate, desiredSpanSec)
    lateinit var scope: CoroutineScope

    init {
        val format = MediaFormat.createVideoFormat("video/avc", width, height)

        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        encoder = MediaCodec.createEncoderByType("video/avc")
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        encoder.start()

        bufferInfo = MediaCodec.BufferInfo()
    }

    fun transferBuffer() {
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            var encoderOutputBuffers = encoder.outputBuffers
            while (true) {
                when (val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> break
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        encoderOutputBuffers = encoder.outputBuffers
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        encodedFormat = encoder.outputFormat
                    }
                    in Int.MIN_VALUE until 0 -> {
                        Log.d("Encoder", "dequeOutputBufferError")
                    }
                    else -> {
                        val encodedData = encoderOutputBuffers[encoderStatus]
                            ?: throw Exception("encoderOutputBuffer[encoderStatus] is null")
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) bufferInfo.size =
                            0
                        if (bufferInfo.size != 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            encoderBuffer.add(
                                encodedData,
                                bufferInfo.flags,
                                bufferInfo.presentationTimeUs
                            )
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
        }
    }

    fun saveVideo(outputFile: File?): Int {
        var index = encoderBuffer.firstIndex
        if (index < 0) {
            return 1
        } else {
            val info = MediaCodec.BufferInfo()
            lateinit var muxer: MediaMuxer
            var result = -1
            try {
                muxer =
                    MediaMuxer(outputFile!!.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val videoTrack = muxer.addTrack(encodedFormat!!)
                muxer.start()
                do {
                    val buffer = encoderBuffer.getChunk(index, info)
                    muxer.writeSampleData(videoTrack, buffer, info)
                    index = encoderBuffer.getNextIndex(index)
                } while (index >= 0)
                result = 0
            } catch (e: Exception) {
                Log.w("Encoder", "saveVideo", e)
                result = 2
            } finally {
                muxer.stop()
                muxer.release()
            }
            return result
        }
    }

    fun shutdown() {
        scope.cancel()
        encoder.stop()
        encoder.release()
    }
}