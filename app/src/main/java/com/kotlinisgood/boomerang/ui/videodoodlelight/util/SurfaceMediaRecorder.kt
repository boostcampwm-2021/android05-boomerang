package com.kotlinisgood.boomerang.ui.videodoodlelight.util

import android.content.Context
import android.graphics.Canvas
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class extends [MediaRecorder] and manages to compose each video frame for recording.
 * Two extra initialization steps before [.start],
 * <pre>
 * [.setWorkerLooper]
 * [.setVideoFrameDrawer]
</pre> *
 *
 * Also you can use it as same as [MediaRecorder] for other functions.
 *
 *
 *  By the way, one more error type [.MEDIA_RECORDER_ERROR_SURFACE] is defined for surface error.
 *
 * Created by z4hyoung on 2017/11/8.
 */
open class SurfaceMediaRecorder : MediaRecorder{
    /**
     * Interface defined for user to customize video frame composition
     */
    interface VideoFrameDrawer {
        /**
         * Called when video frame is composing
         *
         * @param canvas the canvas on which content will be drawn
         */
        fun onDraw(canvas: Canvas?)
    }

    constructor()
    @RequiresApi(Build.VERSION_CODES.S)
    constructor(context: Context) : super(context)

    private var videoSource = 0
    private var onErrorListener: OnErrorListener? = null
    private var interframeGap = DEFAULT_INTER_FRAME_GAP // 1000 milliseconds as default
    private var mSurface: Surface? = null

    // if set, this class works same as MediaRecorder
    private var inputSurface: Surface? = null
    private var workerHandler: Handler? = null
    private var videoFrameDrawer: VideoFrameDrawer? = null

    // indicate surface composing started or not
    private val started = AtomicBoolean(false)

    // indicate surface composing paused or not
    private val paused = AtomicBoolean(false)
    private val workerRunnable: Runnable = object : Runnable {
        private fun handlerCanvasError(errorCode: Int) {
            try {
                stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (onErrorListener != null) {
                onErrorListener!!.onError(
                    this@SurfaceMediaRecorder,
                    MEDIA_RECORDER_ERROR_SURFACE,
                    errorCode
                )
            }
        }

        override fun run() {
            if (!isRecording) {
                return
            }
            var errorCode: Int? = null
            val start = SystemClock.elapsedRealtime()
            do {
                var canvas: Canvas?
                try {
                    canvas = mSurface!!.lockCanvas(null)
                } catch (e: Exception) {
                    errorCode = MEDIA_RECORDER_ERROR_CODE_LOCK_CANVAS
                    e.printStackTrace()
                    break
                }
                videoFrameDrawer!!.onDraw(canvas)
                try {
                    mSurface!!.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    errorCode = MEDIA_RECORDER_ERROR_CODE_UNLOCK_CANVAS
                    e.printStackTrace()
                    break
                }
            } while (false)
            if (!isRecording) {
                return
            }
            errorCode?.let { handlerCanvasError(it) }
                ?: // delay will be reset to 0 if negative in Handler:sendMessageDelayed
                workerHandler!!.postDelayed(
                    this,
                    start + interframeGap - SystemClock.elapsedRealtime()
                )
        }
    }

    @Throws(IllegalStateException::class)
    override fun pause() {
        if (isSurfaceAvailable) {
            paused.set(true)
            workerHandler!!.removeCallbacks(workerRunnable)
        }
        super.pause()
    }

    override fun reset() {
        localReset()
        super.reset()
    }

    @Throws(IllegalStateException::class)
    override fun resume() {
        super.resume()
        if (isSurfaceAvailable) {
            paused.set(false)
            workerHandler!!.post(workerRunnable)
        }
    }

    override fun setOnErrorListener(l: OnErrorListener) {
        super.setOnErrorListener(l)
        onErrorListener = l
    }

    override fun setInputSurface(surface: Surface) {
        super.setInputSurface(surface)
        inputSurface = surface
    }

    @Throws(IllegalStateException::class)
    override fun setVideoFrameRate(rate: Int) {
        super.setVideoFrameRate(rate)
        interframeGap = (1000 / rate + if (1000 % rate == 0) 0 else 1).toLong()
    }

    @Throws(IllegalStateException::class)
    override fun setVideoSource(video_source: Int) {
        super.setVideoSource(video_source)
        videoSource = video_source
    }

    @Throws(IllegalStateException::class)
    override fun start() {
        if (isSurfaceAvailable) {
            checkNotNull(workerHandler) { "worker looper is not initialized yet" }
            checkNotNull(videoFrameDrawer) { "video frame drawer is not initialized yet" }
        }
        super.start()
        if (isSurfaceAvailable) {
            mSurface = surface
            started.set(true)
            workerHandler!!.post(workerRunnable)
        }
    }

    @Throws(IllegalStateException::class)
    override fun stop() {
        localReset()
        super.stop()
    }

    /**
     * Sets video frame drawer for composing.
     * @param drawer the drawer to compose frame with [Canvas]
     * @throws IllegalStateException if it is called after [.start]
     */
    @Throws(IllegalStateException::class)
    fun setVideoFrameDrawer(drawer: VideoFrameDrawer) {
        check(!isRecording) { "setVideoFrameDrawer called in an invalid state: Recording" }
        videoFrameDrawer = drawer
    }

    /**
     * Sets worker looper in which composing task executed
     * @param looper the looper for composing
     * @throws IllegalStateException if it is called after [.start]
     */
    @Throws(IllegalStateException::class)
    fun setWorkerLooper(looper: Looper) {
        check(!isRecording) { "setWorkerLooper called in an invalid state: Recording" }
        workerHandler = Handler(looper)
    }

    /**
     * Returns whether Surface is editable
     * @return true if surface editable
     */
    protected val isSurfaceAvailable: Boolean
        get() = videoSource == VideoSource.SURFACE && inputSurface == null
    private val isRecording: Boolean
        get() = started.get() && !paused.get()

    private fun localReset() {
        if (isSurfaceAvailable) {
            started.compareAndSet(true, false)
            paused.compareAndSet(true, false)
            if (workerHandler != null) {
                workerHandler!!.removeCallbacks(workerRunnable)
            }
        }
        interframeGap = DEFAULT_INTER_FRAME_GAP
        inputSurface = null
        onErrorListener = null
        videoFrameDrawer = null
        workerHandler = null
    }

    companion object {
        /**
         * Surface error during recording, In this case, the application must release the
         * MediaRecorder object and instantiate a new one.
         *
         * @see MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_SURFACE = 10000

        /**
         * Surface error when getting for drawing into this [Surface].
         *
         * @see MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_CODE_LOCK_CANVAS = 1

        /**
         * Surface error when releasing and posting content to [Surface].
         *
         * @see MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_CODE_UNLOCK_CANVAS = 2

        /**
         * default inter-frame gap
         */
        private const val DEFAULT_INTER_FRAME_GAP: Long = 1000
    }
}