package com.kotlinisgood.boomerang.ui.videodoodle

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Surface
import java.io.File
import java.io.IOException
import java.lang.RuntimeException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * Encodes video in a fixed-size circular buffer.
 *
 *
 * The obvious way to do this would be to store each packet in its own buffer and hook it
 * into a linked list.  The trouble with this approach is that it requires constant
 * allocation, which means we'll be driving the GC to distraction as the frame rate and
 * bit rate increase.  Instead we create fixed-size pools for video data and metadata,
 * which requires a bit more work for us but avoids allocations in the steady state.
 *
 *
 * Video must always start with a sync frame (a/k/a key frame, a/k/a I-frame).  When the
 * circular buffer wraps around, we either need to delete all of the data between the frame at
 * the head of the list and the next sync frame, or have the file save function know that
 * it needs to scan forward for a sync frame before it can start saving data.
 *
 *
 * When we're told to save a snapshot, we create a MediaMuxer, write all the frames out,
 * and then go back to what we were doing.
 */
class CircularEncoder(
    width: Int, height: Int, bitRate: Int, frameRate: Int, desiredSpanSec: Int,
    cb: Callback
) {
    private val mEncoderThread: EncoderThread

    /**
     * Returns the encoder's input surface.
     */
    val inputSurface: Surface
    private var mEncoder: MediaCodec?

    /**
     * Callback function definitions.  CircularEncoder caller must provide one.
     */
    interface Callback {
        /**
         * Called some time after saveVideo(), when all data has been written to the
         * output file.
         *
         * @param status Zero means success, nonzero indicates failure.
         */
        fun fileSaveComplete(status: Int)

        /**
         * Called occasionally.
         *
         * @param totalTimeMsec Total length, in milliseconds, of buffered video.
         */
        fun bufferStatus(totalTimeMsec: Long)
    }

    /**
     * Shuts down the encoder thread, and releases encoder resources.
     *
     *
     * Does not return until the encoder thread has stopped.
     */
    fun shutdown() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects")
        val handler: Handler? = mEncoderThread.handler
        handler!!.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SHUTDOWN))
        try {
            mEncoderThread.join()
        } catch (ie: InterruptedException) {
            Log.w(TAG, "Encoder thread join() was interrupted", ie)
        }
        if (mEncoder != null) {
            mEncoder!!.stop()
            mEncoder!!.release()
            mEncoder = null
        }
    }

    /**
     * Notifies the encoder thread that a new frame will shortly be provided to the encoder.
     *
     *
     * There may or may not yet be data available from the encoder output.  The encoder
     * has a fair mount of latency due to processing, and it may want to accumulate a
     * few additional buffers before producing output.  We just need to drain it regularly
     * to avoid a situation where the producer gets wedged up because there's no room for
     * additional frames.
     *
     *
     * If the caller sends the frame and then notifies us, it could get wedged up.  If it
     * notifies us first and then sends the frame, we guarantee that the output buffers
     * were emptied, and it will be impossible for a single additional frame to block
     * indefinitely.
     */
    fun frameAvailableSoon() {
        val handler: Handler? = mEncoderThread.handler
        handler!!.sendMessage(
            handler.obtainMessage(
                EncoderThread.EncoderHandler.MSG_FRAME_AVAILABLE_SOON
            )
        )
    }

    /**
     * Initiates saving the currently-buffered frames to the specified output file.  The
     * data will be written as a .mp4 file.  The call returns immediately.  When the file
     * save completes, the callback will be notified.
     *
     *
     * The file generation is performed on the encoder thread, which means we won't be
     * draining the output buffers while this runs.  It would be wise to stop submitting
     * frames during this time.
     */
    fun saveVideo(outputFile: File?) {
        val handler: Handler? = mEncoderThread.handler
        handler!!.sendMessage(
            handler.obtainMessage(
                EncoderThread.EncoderHandler.MSG_SAVE_VIDEO, outputFile
            )
        )
    }

    /**
     * Object that encapsulates the encoder thread.
     *
     *
     * We want to sleep until there's work to do.  We don't actually know when a new frame
     * arrives at the encoder, because the other thread is sending frames directly to the
     * input surface.  We will see data appear at the decoder output, so we can either use
     * an infinite timeout on dequeueOutputBuffer() or wait() on an object and require the
     * calling app wake us.  It's very useful to have all of the buffer management local to
     * this thread -- avoids synchronization -- so we want to do the file muxing in here.
     * So, it's best to sleep on an object and do something appropriate when awakened.
     *
     *
     * This class does not manage the MediaCodec encoder startup/shutdown.  The encoder
     * should be fully started before the thread is created, and not shut down until this
     * thread has been joined.
     */
    private class EncoderThread(
        private val mEncoder: MediaCodec, encBuffer: CircularEncoderBuffer,
        callback: Callback
    ) : Thread() {
        private var mEncodedFormat: MediaFormat? = null
        private val mBufferInfo: MediaCodec.BufferInfo
        private var mHandler: EncoderHandler? = null
        private val mEncBuffer: CircularEncoderBuffer
        private val mCallback: Callback
        private var mFrameNum = 0
        private val mLock = ReentrantLock()
        private val condition = mLock.newCondition()

        @Volatile
        private var mReady = false

        /**
         * Thread entry point.
         *
         *
         * Prepares the Looper, Handler, and signals anybody watching that we're ready to go.
         */
        override fun run() {
            Looper.prepare()
            mHandler = EncoderHandler(this) // must create on encoder thread
            Log.d(TAG, "encoder thread ready")
            mLock.withLock {
                mReady = true
                condition.signal() // signal waitUntilReady()
            }
            Looper.loop()
            synchronized(mLock) {
                mReady = false
                mHandler = null
            }
            Log.d(TAG, "looper quit")
        }

        /**
         * Waits until the encoder thread is ready to receive messages.
         *
         *
         * Call from non-encoder thread.
         */
        fun waitUntilReady() {
            mLock.withLock {
                while (!mReady) {
                    try {
                        condition.await()
                    } catch (ie: InterruptedException) { /* not expected */
                    }
                }
            }
        }// Confirm ready state.

        /**
         * Returns the Handler used to send messages to the encoder thread.
         */
        val handler: EncoderHandler?
            get() {
                synchronized(mLock) {
                    // Confirm ready state.
                    if (!mReady) {
                        throw RuntimeException("not ready")
                    }
                }
                return mHandler
            }

        /**
         * Drains all pending output from the decoder, and adds it to the circular buffer.
         */
        fun drainEncoder() {
            val TIMEOUT_USEC = 0 // no timeout -- check for buffers, bail if none
            var encoderOutputBuffers = mEncoder.outputBuffers
            while (true) {
                val encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC.toLong())
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    break
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.outputBuffers
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Should happen before receiving buffers, and should only happen once.
                    // The MediaFormat contains the csd-0 and csd-1 keys, which we'll need
                    // for MediaMuxer.  It's unclear what else MediaMuxer might want, so
                    // rather than extract the codec-specific data and reconstruct a new
                    // MediaFormat later, we just grab it here and keep it around.
                    mEncodedFormat = mEncoder.outputFormat
                    Log.d(
                        TAG,
                        "encoder output format changed: $mEncodedFormat"
                    )
                } else if (encoderStatus < 0) {
                    Log.w(
                        TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                                encoderStatus
                    )
                    // let's ignore it
                } else {
                    val encodedData = encoderOutputBuffers.get(encoderStatus)
                        ?: throw RuntimeException(
                            ("encoderOutputBuffer " + encoderStatus +
                                    " was null")
                        )
                    if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // The codec config data was pulled out when we got the
                        // INFO_OUTPUT_FORMAT_CHANGED status.  The MediaMuxer won't accept
                        // a single big blob -- it wants separate csd-0/csd-1 chunks --
                        // so simply saving this off won't work.
                        if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
                        mBufferInfo.size = 0
                    }
                    if (mBufferInfo.size != 0) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset)
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size)
                        mEncBuffer.add(
                            encodedData, mBufferInfo.flags,
                            mBufferInfo.presentationTimeUs
                        )
                        if (VERBOSE) {
                            Log.d(
                                TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                        mBufferInfo.presentationTimeUs
                            )
                        }
                    }
                    mEncoder.releaseOutputBuffer(encoderStatus, false)
                    if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Log.w(TAG, "reached end of stream unexpectedly")
                        break // out of while
                    }
                }
            }
        }

        /**
         * Drains the encoder output.
         *
         *
         * See notes for [CircularEncoder.frameAvailableSoon].
         */
        fun frameAvailableSoon() {
            if (VERBOSE) Log.d(TAG, "frameAvailableSoon")
            drainEncoder()
            mFrameNum++
            if (mFrameNum % 10 == 0) {        // should base off frame rate or clock?
                mCallback.bufferStatus(mEncBuffer.computeTimeSpanUsec())
            }
        }

        /**
         * Saves the encoder output to a .mp4 file.
         *
         *
         * We'll drain the encoder to get any lingering data, but we're not going to shut
         * the encoder down or use other tricks to try to "flush" the encoder.  This may
         * mean we miss the last couple of submitted frames if they're still working their
         * way through.
         *
         *
         * We may want to reset the buffer after this -- if they hit "capture" again right
         * away they'll end up saving video with a gap where we paused to write the file.
         */
        fun saveVideo(outputFile: File) {
            if (VERBOSE) Log.d(
                TAG,
                "saveVideo $outputFile"
            )
            var index: Int = mEncBuffer.firstIndex
            if (index < 0) {
                Log.w(TAG, "Unable to get first index")
                mCallback.fileSaveComplete(1)
                return
            }
            val info = MediaCodec.BufferInfo()
            var muxer: MediaMuxer? = null
            var result = -1
            try {
                muxer = MediaMuxer(
                    outputFile.path,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                val videoTrack = muxer.addTrack(mEncodedFormat!!)
                muxer.start()
                do {
                    val buf: ByteBuffer = mEncBuffer.getChunk(index, info)
                    if (VERBOSE) {
                        Log.d(TAG, "SAVE " + index + " flags=0x" + Integer.toHexString(info.flags))
                    }
                    muxer.writeSampleData(videoTrack, buf, info)
                    index = mEncBuffer.getNextIndex(index)
                } while (index >= 0)
                result = 0
            } catch (ioe: IOException) {
                Log.w(TAG, "muxer failed", ioe)
                result = 2
            } finally {
                if (muxer != null) {
                    muxer.stop()
                    muxer.release()
                }
            }
            if (VERBOSE) {
                Log.d(TAG, "muxer stopped, result=$result")
            }
            mCallback.fileSaveComplete(result)
        }

        /**
         * Tells the Looper to quit.
         */
        fun shutdown() {
            if (VERBOSE) Log.d(TAG, "shutdown")
            Looper.myLooper()!!.quit()
        }

        /**
         * Handler for EncoderThread.  Used for messages sent from the UI thread (or whatever
         * is driving the encoder) to the encoder thread.
         *
         *
         * The object is created on the encoder thread.
         */
        class EncoderHandler(et: EncoderThread) : Handler() {
            // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
            // but no real harm in it.
            private val mWeakEncoderThread: WeakReference<EncoderThread>

            // runs on encoder thread
            override fun handleMessage(msg: Message) {
                val what = msg.what
                if (VERBOSE) {
                    Log.v(TAG, "EncoderHandler: what=$what")
                }
                val encoderThread = mWeakEncoderThread.get()
                if (encoderThread == null) {
                    Log.w(TAG, "EncoderHandler.handleMessage: weak ref is null")
                    return
                }
                when (what) {
                    MSG_FRAME_AVAILABLE_SOON -> encoderThread.frameAvailableSoon()
                    MSG_SAVE_VIDEO -> encoderThread.saveVideo(msg.obj as File)
                    MSG_SHUTDOWN -> encoderThread.shutdown()
                    else -> throw RuntimeException("unknown message $what")
                }
            }

            companion object {
                val MSG_FRAME_AVAILABLE_SOON = 1
                val MSG_SAVE_VIDEO = 2
                val MSG_SHUTDOWN = 3
            }

            /**
             * Constructor.  Instantiate object from encoder thread.
             */
            init {
                mWeakEncoderThread = WeakReference(et)
            }
        }

        init {
            mEncBuffer = encBuffer
            mCallback = callback
            mBufferInfo = MediaCodec.BufferInfo()
        }
    }

    companion object {
        private val TAG: String = "CircularEncoderTAG"
        private val VERBOSE = false
        private val MIME_TYPE = "video/avc" // H.264 Advanced Video Coding
        private val IFRAME_INTERVAL = 1 // sync frame every second
    }

    /**
     * Configures encoder, and prepares the input Surface.
     *
     * @param width Width of encoded video, in pixels.  Should be a multiple of 16.
     * @param height Height of encoded video, in pixels.  Usually a multiple of 16 (1080 is ok).
     * @param bitRate Target bit rate, in bits.
     * @param frameRate Expected frame rate.
     * @param desiredSpanSec How many seconds of video we want to have in our buffer at any time.
     */
    init {
        // The goal is to size the buffer so that we can accumulate N seconds worth of video,
        // where N is passed in as "desiredSpanSec".  If the codec generates data at roughly
        // the requested bit rate, we can compute it as time * bitRate / bitsPerByte.
        //
        // Sync frames will appear every (frameRate * IFRAME_INTERVAL) frames.  If the frame
        // rate is higher or lower than expected, various calculations may not work out right.
        //
        // Since we have to start muxing from a sync frame, we want to ensure that there's
        // room for at least one full GOP in the buffer, preferrably two.
        if (desiredSpanSec < IFRAME_INTERVAL * 2) {
            throw RuntimeException(
                "Requested time span is too short: " + desiredSpanSec +
                        " vs. " + IFRAME_INTERVAL * 2
            )
        }
        val encBuffer = CircularEncoderBuffer(
            bitRate, frameRate,
            desiredSpanSec
        )
        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height)

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        if (VERBOSE) Log.d(
            TAG,
            "format: $format"
        )

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE)
        mEncoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = mEncoder!!.createInputSurface()
        mEncoder!!.start()

        // Start the encoder thread last.  That way we're sure it can see all of the state
        // we've initialized.
        mEncoderThread = EncoderThread(mEncoder!!, encBuffer, cb)
        mEncoderThread.start()
        mEncoderThread.waitUntilReady()
    }
}