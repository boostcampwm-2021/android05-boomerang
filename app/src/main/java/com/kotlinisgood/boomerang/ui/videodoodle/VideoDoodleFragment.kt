package com.kotlinisgood.boomerang.ui.videodoodle

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLES20
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.*
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoDoodleBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import java.io.File
import java.io.IOException
import java.lang.RuntimeException
import java.lang.ref.WeakReference
import kotlin.system.measureNanoTime


class VideoDoodleFragment : Fragment(), SurfaceHolder.Callback,
    SurfaceTexture.OnFrameAvailableListener {

    private lateinit var binding: FragmentVideoDoodleBinding

    private val args: VideoDoodleFragmentArgs by navArgs()
    private val path by lazy { args.videoPath }

    private var eglCore: EglCore? = null
    private var displaySurface: WindowSurface? = null
    private var encoderSurface: WindowSurface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private lateinit var surfaceView: SurfaceView
    private var textureId = 0
    private var fullFrameBlit: FullFrameRect? = null
    private val mTmpMatrix = FloatArray(16)
    private lateinit var circularEncoder: CircularEncoder

    private lateinit var mediaPlayer: MediaPlayer
    private var videoWidth = 0
    private var videoHeight = 0
    private var outputVideo: File? = null
//    private var secondsVideo = 0f

    private lateinit var handler: MainHandler

    var currentPoint: MutableList<Pair<Int, Int>> = mutableListOf()

    var isSurfaceDestroyed = false

    private class MainHandler(fragment: VideoDoodleFragment) :
        Handler(Looper.getMainLooper()), CircularEncoder.Callback {
        private val weakReference: WeakReference<VideoDoodleFragment> =
            WeakReference<VideoDoodleFragment>(fragment)

        override fun fileSaveComplete(status: Int) {
            sendMessage(obtainMessage(MSG_SAVE_COMPLETE, status, 0, null))
        }

//        override fun bufferStatus(totalTimeMsec: Long) {
//            sendMessage(
//                obtainMessage(
//                    MSG_BUFFER_STATUS,
//                    (totalTimeMsec shr 32).toInt(), totalTimeMsec.toInt()
//                )
//            )
//        }

        override fun handleMessage(msg: Message) {
            val fragment: VideoDoodleFragment? = weakReference.get()
            if (fragment == null) {
                Log.d(
                    TAG,
                    "Got message for dead fragment"
                )
                return
            }
            when (msg.what) {
                MSG_FRAME_AVAILABLE -> {
                    fragment.drawFrame()
                }
                MSG_BUFFER_STATUS -> {
//                    val duration = msg.arg1.toLong() shl 32 or
//                            (msg.arg2.toLong() and 0xffffffffL)
//                    fragment.updateBufferStatus(duration)
                }
                MSG_SAVE_COMPLETE -> {
                    fragment.saveCompleted(msg.arg1)
                }
                else -> throw RuntimeException("Unknown message " + msg.what)
            }
        }

        companion object {
            const val MSG_FRAME_AVAILABLE = 0
            const val MSG_BUFFER_STATUS = 1
            const val MSG_SAVE_COMPLETE = 2
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_video_doodle, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        surfaceView = binding.svMovie
        surfaceView.holder.addCallback(this)

        handler = MainHandler(this)
        outputVideo = File(requireContext().filesDir, "outputVideo.mp4")

        binding.btnPlay.setOnClickListener {
            playVideoAlt()
        }

        binding.btnCapture.setOnClickListener {
            circularEncoder.saveVideo(outputVideo)
        }

        binding.svMovie.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    drawLine(motionEvent.x.toInt(), motionEvent.y.toInt())
                }
                MotionEvent.ACTION_MOVE -> {
                    fillSpace(motionEvent.x.toInt(), motionEvent.y.toInt())
//                    drawLine(motionEvent.x.toInt(), motionEvent.y.toInt())
                }
                MotionEvent.ACTION_UP -> {
                }
            }
            binding.svMovie.performClick()
            true
        }
    }

    private fun drawLine(x: Int, y: Int) {
        currentPoint.add(Pair(x, y))
    }

    private fun fillSpace(x: Int, y: Int) {
        val last = currentPoint.last()
        for (i in 1..50) {
            currentPoint.add(
                Pair(
                    last.first + (x - last.first) * i / 100,
                    last.second + (y - last.second) * i / 100
                )
            )
        }
    }

    private fun saveCompleted(status: Int) {
        if (status == 0) {
            Log.d(TAG, "Save Completed")
            val action =
                VideoDoodleFragmentDirections.actionVideoDoodleFragmentToVideoEditLightFragment(
                    outputVideo!!.absolutePath,
                    mutableListOf<SubVideo>().toTypedArray()
                )
            findNavController().navigate(action)
        } else {
            Log.d(TAG, "Save Failed")
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.stop()
        mediaPlayer.release()
        circularEncoder.shutdown()
        encoderSurface?.release()
        encoderSurface = null
        surfaceTexture?.release()
        surfaceTexture = null
        displaySurface?.release()
        displaySurface = null
        fullFrameBlit?.release(false)
        fullFrameBlit = null
        eglCore?.release()
        eglCore = null
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated: surfaceHolder=$p0")

        eglCore = EglCore()
        displaySurface = WindowSurface(eglCore!!, p0.surface, false)
        displaySurface!!.makeCurrent()

        fullFrameBlit = FullFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
        textureId = fullFrameBlit!!.createTextureObject()
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture!!.setOnFrameAvailableListener(this)

        val width = binding.svMovie.width

        try {
            circularEncoder = CircularEncoder(width, width, 6000000, 30, 60, handler)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        encoderSurface = WindowSurface(eglCore!!, circularEncoder.inputSurface, true)
    }

    private fun playVideoAlt() {
        val surface = Surface(surfaceTexture)

        mediaPlayer = MediaPlayer.create(context, path.toUri())
        mediaPlayer.setSurface(surface)
        videoWidth = mediaPlayer.videoWidth
        videoHeight = mediaPlayer.videoHeight
        mediaPlayer.start()
        surface.release()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        Log.d(TAG, "surfaceChanged: format=$p1, width=$p2, height=$p3")
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed: holder=$p0")
        isSurfaceDestroyed = true
    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {
        Log.d(TAG, "onFrameAvailable: surfaceTexture=$p0")
//        if (!isSurfaceDestroyed) drawFrame()
        if (!isSurfaceDestroyed) handler.sendEmptyMessage(MainHandler.MSG_FRAME_AVAILABLE)
    }

    private fun drawFrame() {
        val width = binding.svMovie.width
        val height = binding.svMovie.height

        displaySurface?.makeCurrent()
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(mTmpMatrix)

//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, )

//        width, height가 videoWidth, VideoHeight보다 더 크다는 가정하에 함. 그 외의 경우도 만들어야 함!
//        SurfaceView에 그리기
        if (videoWidth > videoHeight) {
            val adjustedHeight = (height * (videoHeight / videoWidth.toFloat())).toInt()
            GLES20.glViewport(0, (height - adjustedHeight) / 2, width, adjustedHeight)
        } else {
            val adjustedWidth = (width * (videoWidth / videoHeight.toFloat())).toInt()
            GLES20.glViewport((width - adjustedWidth) / 2, 0, adjustedWidth, height)
        }
        fullFrameBlit?.drawFrame(textureId, mTmpMatrix)
        drawExtra(currentPoint, height)
        displaySurface?.swapBuffers()

//        저장하기
        circularEncoder.frameAvailableSoon()
        encoderSurface?.makeCurrent()
        if (videoWidth > videoHeight) {
            val adjustedHeight = (height * (videoHeight / videoWidth.toFloat())).toInt()
            GLES20.glViewport(0, (height - adjustedHeight) / 2, width, adjustedHeight)
        } else {
            val adjustedWidth = (width * (videoWidth / videoHeight.toFloat())).toInt()
            GLES20.glViewport((width - adjustedWidth) / 2, 0, adjustedWidth, height)
        }
        fullFrameBlit?.drawFrame(textureId, mTmpMatrix)
        drawExtra(currentPoint, height)
        encoderSurface?.setPresentationTime(surfaceTexture!!.timestamp)
        encoderSurface?.swapBuffers()
    }

    private fun drawExtra(currentPoint: List<Pair<Int, Int>>, height: Int) {
        val time = measureNanoTime {
            currentPoint.forEach {
                GLES20.glClearColor(1f, 1f, 0f, 1f)
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
                GLES20.glScissor(it.first, height - it.second, 15, 15)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            }
        }
        println(time)
    }

    companion object {
        private const val TAG = "VideoDoodleFragmentTAG"
    }
}