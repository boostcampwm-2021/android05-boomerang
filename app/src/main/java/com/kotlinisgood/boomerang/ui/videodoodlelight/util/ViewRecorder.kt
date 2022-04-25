package com.kotlinisgood.boomerang.ui.videodoodlelight.util

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Looper
import android.util.Size
import android.view.View
import androidx.annotation.RequiresApi
import kotlin.math.min

/**
 * Used to record a video with view that can be captured. It also supports to switch views during recording.
 * This class extends [SurfaceMediaRecorder] and provides an extra API [.setRecordedView].
 *
 *
 * By default, capture is drawn in the center of canvas in scale if necessary.
 * It is easy to change drawing behavior with [.setVideoFrameDrawer].
 *
 *
 * Main thread is set for drawing as capture is only available in this thread,
 * it's OK to move composing to a background thread with [.setWorkerLooper],
 * in this case, a capture buffer for multi-thread may be required.
 *
 * Created by z4hyoung on 2017/11/8.
 */
class ViewRecorder : SurfaceMediaRecorder {

    constructor()
    @RequiresApi(Build.VERSION_CODES.S)
    constructor(context: Context) : super(context)

    private var recordedView: View? = null
    private var videoSize: Size? = null
    private val frameDrawer: VideoFrameDrawer = object : VideoFrameDrawer {
        private fun getMatrix(bw: Int, bh: Int, vw: Int, vh: Int): Matrix {
            val matrix = Matrix()
            val scale: Float
            var scaleX = 1f
            var scaleY = 1f
            if (bw > vw) {
                scaleX = vw.toFloat() / bw
            }
            if (bh > vh) {
                scaleY = vh.toFloat() / bh
            }
            scale = min(scaleX, scaleY)
            val transX: Float = (vw - bw * scale) / 2
            val transY: Float = (vh - bh * scale) / 2
            matrix.postScale(scale, scale)
            matrix.postTranslate(transX, transY)
            return matrix
        }

        override fun onDraw(canvas: Canvas?) {
            val bitmap = getBitmapFromView(recordedView!!)
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            val videoWidth = videoSize!!.width
            val videoHeight = videoSize!!.height
            val matrix = getMatrix(bitmapWidth, bitmapHeight, videoWidth, videoHeight)
            canvas!!.drawColor(Color.BLACK, PorterDuff.Mode.SRC_OVER)
            canvas.drawBitmap(bitmap, matrix, null)
        }
    }

    fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    @Throws(IllegalStateException::class)
    override fun setVideoSize(width: Int, height: Int) {
        super.setVideoSize(width, height)
        videoSize = Size(width, height)
    }

    @Throws(IllegalStateException::class)
    override fun start() {
        if (isSurfaceAvailable) {
            checkNotNull(videoSize) { "video size is not initialized yet" }
            checkNotNull(recordedView) { "recorded view is not initialized yet" }
            setWorkerLooper(Looper.getMainLooper())
            setVideoFrameDrawer(frameDrawer)
        }
        super.start()
    }

    /**
     * Sets recorded view to be captured for video frame composition. Call this method before start().
     * You may change the recorded view with this method during recording.
     *
     * @param view the view to be captured
     */
    @Throws(IllegalStateException::class)
    fun setRecordedView(view: View) {
        recordedView = view
    }
}