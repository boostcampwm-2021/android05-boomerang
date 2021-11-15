package com.kotlinisgood.boomerang.ui.videodoodle

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLES30
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


class VideoDoodleFragment : Fragment(), SurfaceHolder.Callback,
    SurfaceTexture.OnFrameAvailableListener {

    private lateinit var binding: FragmentVideoDoodleBinding

    private val args: VideoDoodleFragmentArgs by navArgs()
    private val path by lazy { args.videoPath }

    private var eglCore: EglCore? = null
    private var displaySurface: EglWindowSurface? = null
    private var encoderSurface: EglWindowSurface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private lateinit var surfaceView: SurfaceView
    private var textureId = 0
    private var fullFrameBlit: FullFrameRect? = null
    private val mTmpMatrix = FloatArray(16)
    private lateinit var circularEncoder: CircularEncoder

    private lateinit var mediaPlayer: MediaPlayer
    private var videoWidth = 0
    private var videoHeight = 0
    private var width = 0
    private var height = 0
    private var outputVideo: File? = null

    //    private var secondsVideo = 0f
    private var viewportX = 0
    private var viewportY = 0
    private var viewportWidth = 0
    private var viewportHeight = 0

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
        val currentUnixTime = System.currentTimeMillis()
        outputVideo = File(requireContext().filesDir, "${currentUnixTime}.mp4")

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

//        EGL 설정
        eglCore = EglCore()
        displaySurface = EglWindowSurface(eglCore!!, p0.surface, false)
        displaySurface!!.makeCurrent()

        fullFrameBlit = FullFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
        textureId = fullFrameBlit!!.createTextureObject()
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture!!.setOnFrameAvailableListener(this)

        val surface = Surface(surfaceTexture)
        mediaPlayer = MediaPlayer.create(context, path.toUri())
        mediaPlayer.setSurface(surface)
        videoWidth = mediaPlayer.videoWidth
        videoHeight = mediaPlayer.videoHeight

        width = binding.svMovie.width
        height = binding.svMovie.height

        //        width, height가 videoWidth, VideoHeight보다 더 크다는 가정하에 함. 그 외의 경우도 만들어야 함!
        if (videoWidth > videoHeight) {
            val adjustedHeight = (height * (videoHeight / videoWidth.toFloat())).toInt()
            viewportX = 0
            viewportY = (height - adjustedHeight) / 2
            viewportWidth = width
            viewportHeight = adjustedHeight
        } else {
            val adjustedWidth = (width * (videoWidth / videoHeight.toFloat())).toInt()
            viewportX = (width - adjustedWidth) / 2
            viewportY = 0
            viewportWidth = adjustedWidth
            viewportHeight = height
        }

        try {
            circularEncoder = CircularEncoder(width, width, 6000000, 30, 60, handler)
        } catch (e: IOException) {
            throw Exception(e)
        }
        encoderSurface = EglWindowSurface(eglCore!!, circularEncoder.inputSurface, true)
    }

    private fun playVideoAlt() {
        mediaPlayer.start()
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
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(mTmpMatrix)

//        SurfaceView에 그리기
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
        fullFrameBlit?.drawFrame(textureId, mTmpMatrix)
        drawLine(currentPoint, height)

        if (eglCore?.glVersion == 3) {
//        SurfaceView에 그릴 Framebuffer를 아직 swap하지 말고 인코딩 버퍼에 복사하고 둘 다 swap
            encoderSurface?.makeCurrentReadFrom(displaySurface!!)
            GLES30.glBlitFramebuffer(
                0,
                0,
                displaySurface!!.width,
                displaySurface!!.height,
                0,
                0,
                displaySurface!!.width,
                displaySurface!!.height,
                GLES30.GL_COLOR_BUFFER_BIT,
                GLES30.GL_NEAREST
            )
            circularEncoder.frameAvailableSoon()
            encoderSurface?.setPresentationTime(surfaceTexture!!.timestamp)
            encoderSurface?.swapBuffers()

            displaySurface?.makeCurrent()
            displaySurface?.swapBuffers()
        } else {
//            OpenGL ES 2.0일 경우. glBlitFramebuffer를 지원하지 않기에 그냥 두 번 그린다.
            displaySurface?.swapBuffers()

            encoderSurface?.makeCurrent()
            GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
            fullFrameBlit?.drawFrame(textureId, mTmpMatrix)
            drawLine(currentPoint, height)
            circularEncoder.frameAvailableSoon()
            encoderSurface?.setPresentationTime(surfaceTexture!!.timestamp)
            encoderSurface?.swapBuffers()
            displaySurface?.makeCurrent()
        }
    }

    private fun drawLine(currentPoint: List<Pair<Int, Int>>, height: Int) {
        currentPoint.forEach {
            GLES20.glClearColor(1f, 1f, 0f, 1f)
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
            GLES20.glScissor(it.first, height - it.second, 15, 15)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        }
    }

    companion object {
        private const val TAG = "VideoDoodleFragmentTAG"
    }
}