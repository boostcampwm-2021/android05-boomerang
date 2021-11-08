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

private const val TAG = "VideoDoodleFragment"

class VideoDoodleFragment : Fragment(), SurfaceHolder.Callback,
    SurfaceTexture.OnFrameAvailableListener {

    private val args: VideoDoodleFragmentArgs by navArgs()
    private val path by lazy { args.videoPath }
    private var eglCore: EglCore? = null
    private var displaySurface: WindowSurface? = null
    private var encoderSurface: WindowSurface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var textureId = 0
    private var fullFrameBlit: FullFrameRect? = null

    private lateinit var mediaPlayer: MediaPlayer

    private val mTmpMatrix = FloatArray(16)
    private var videoWidth = 0
    private var videoHeight = 0
    private lateinit var circularEncoder: CircularEncoder

    private lateinit var handler: MainHandler

    private var frameNum = 0
    private var outputVideo: File? = null

    private var secondsVideo = 0f

    private lateinit var binding: FragmentVideoDoodleBinding

    private lateinit var surfaceView: SurfaceView

    var currentPoint: MutableList<Pair<Int, Int>> = mutableListOf()

    private class MainHandler(fragment: VideoDoodleFragment) :
        Handler(Looper.getMainLooper()), CircularEncoder.Callback {
        private val weakReference: WeakReference<VideoDoodleFragment> =
            WeakReference<VideoDoodleFragment>(fragment)

        override fun fileSaveComplete(status: Int) {
        }

        override fun bufferStatus(totalTimeMsec: Long) {
            sendMessage(
                obtainMessage(
                    MSG_BUFFER_STATUS,
                    (totalTimeMsec shr 32).toInt(), totalTimeMsec.toInt()
                )
            )
        }

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
                    val duration = msg.arg1.toLong() shl 32 or
                            (msg.arg2.toLong() and 0xffffffffL)
                    fragment.updateBufferStatus(duration)
                }
                else -> throw RuntimeException("Unknown message " + msg.what)
            }
        }

        companion object {
            const val MSG_FRAME_AVAILABLE = 0
            const val MSG_BUFFER_STATUS = 1
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
            val action = VideoDoodleFragmentDirections.actionVideoDoodleFragmentToVideoEditLightFragment(outputVideo!!.absolutePath, mutableListOf<SubVideo>().toTypedArray())
            findNavController().navigate(action)
        }

        binding.svMovie.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    drawLine(motionEvent.x.toInt(), motionEvent.y.toInt())
                }
                MotionEvent.ACTION_MOVE -> {
//                    drawLine(motionEvent.x.toInt(), motionEvent.y.toInt())
                    fillSpace(motionEvent.x.toInt(), motionEvent.y.toInt())
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
        for (i in 1..100) {
            currentPoint.add(Pair(last.first + (x - last.first)*i/100, last.second + (y - last.second)*i/100))
        }
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated: surfaceHolder=$p0")

        eglCore = EglCore(null, EglCore.FLAG_RECORDABLE)
        displaySurface = WindowSurface(eglCore!!, p0.surface, false)
        displaySurface!!.makeCurrent()

        fullFrameBlit = FullFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
        textureId = fullFrameBlit!!.createTextureObject()
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture!!.setOnFrameAvailableListener(this)

        try {
            circularEncoder = CircularEncoder(1080, 1080, 6000000, 60, 2, handler)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        encoderSurface = WindowSurface(eglCore!!, circularEncoder.inputSurface, true)
    }

    private fun playVideoAlt() {
        val surface = Surface(surfaceTexture)

//        현재 앱별 저장소에서 대상 비디오 파일 가져옴
        mediaPlayer = MediaPlayer.create(
            context,
            path.toUri()
        )
        mediaPlayer.setSurface(surface)
        videoWidth = mediaPlayer.videoWidth
        videoHeight = mediaPlayer.videoHeight
        mediaPlayer.start()
        surface.release()
    }

    private fun updateBufferStatus(durationUsec: Long) {
        secondsVideo = durationUsec / 1000000.0f
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        Log.d(TAG, "surfaceChanged: format=$p1, width=$p2, height=$p3")
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed: holder=$p0")
    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {
        Log.d(TAG, "onFrameAvailable: surfaceTexture=$p0")
        drawFrame()
    }

    private fun drawFrame() {
        val width = binding.svMovie.width
        val height = binding.svMovie.height

        displaySurface?.makeCurrent()
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(mTmpMatrix)

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
        circularEncoder.frameAvailableSoon()
        encoderSurface?.setPresentationTime(surfaceTexture!!.timestamp)
        encoderSurface?.swapBuffers()


        frameNum++
    }

    private fun drawExtra(currentPoint: List<Pair<Int, Int>>, height: Int) {
//        점으로 그려짐
        currentPoint.forEach {
            GLES20.glClearColor(1f, 1f, 0f, 1f)
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
            GLES20.glScissor(it.first, height - it.second, 15, 15)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        }
        println(currentPoint)
//        점 이어서 선 그리기
//        GLES20.glClearColor(1f, 1f, 0f, 1f)
//        val vertices = ByteBuffer.allocateDirect(touchPoints.size * 4).run {
//            order(ByteOrder.nativeOrder())
//            asFloatBuffer().apply {
//                put(touchPoints.toFloatArray())
//                position(0)
//            }
//        }
//        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, vertices)
//        GLES20.glEnableVertexAttribArray(1)
//        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, touchPoints.size / 3)
    }
}