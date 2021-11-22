package com.kotlinisgood.boomerang.ui.videodoodle

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLES30
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoDoodleBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.util.UriUtil
import java.io.File
import java.io.IOException


class VideoDoodleFragment : Fragment(), SurfaceHolder.Callback,
    SurfaceTexture.OnFrameAvailableListener {

    private lateinit var binding: FragmentVideoDoodleBinding

    private val args: VideoDoodleFragmentArgs by navArgs()
    private val uriString by lazy { args.videoPath }

    private var eglCore: EglCore? = null
    private var displaySurface: EglWindowSurface? = null
    private var encoderSurface: EglWindowSurface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private lateinit var surfaceView: SurfaceView
    private var textureId = 0
    private var fullFrameBlit: FullFrameRect? = null
    private val mTmpMatrix = FloatArray(16)
    private lateinit var circularEncoder: Encoder

    private lateinit var mediaPlayer: MediaPlayer
    private var videoWidth = 0
    private var videoHeight = 0
    private var width = 0
    private var height = 0
    private var outputVideo: File? = null

    private var viewportX = 0
    private var viewportY = 0
    private var viewportWidth = 0
    private var viewportHeight = 0

    //    x, y, 색상
    private var currentPoint: MutableList<Triple<Int, Int, DrawColor>> = mutableListOf()
    private var drawColor = DrawColor(red = 1f, green = 0f, blue = 0f)
    private var isPlaying = false
    private var isSurfaceDestroyed = false

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

        val currentUnixTime = System.currentTimeMillis()
        outputVideo = File(requireContext().filesDir, "${currentUnixTime}.mp4")

        val uri = if (Build.VERSION.SDK_INT >= 30) {
            uriString.toUri()
        } else {
            Uri.fromFile(
                File(
                    UriUtil.getPathFromUri(
                        requireActivity().contentResolver,
                        uriString.toUri()
                    )
                )
            )
        }

        mediaPlayer = MediaPlayer.create(context, uri)

        mediaPlayer.setOnPreparedListener {
            binding.btnPlay.isEnabled = true
        }

        binding.btnPlay.setOnClickListener {
            playVideo()
        }

        binding.btnCapture.setOnClickListener {
            saveCompleted(circularEncoder.saveVideo(outputVideo))
        }

        binding.svMovie.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isPlaying) drawLine(motionEvent.x.toInt(), motionEvent.y.toInt())
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isPlaying) fillSpace(motionEvent.x.toInt(), motionEvent.y.toInt())
                }
                MotionEvent.ACTION_UP -> {
                }
            }
            binding.svMovie.performClick()
            true
        }

        binding.rbRed.isChecked = true
        binding.rgDoodleColor.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_red -> drawColor = DrawColor(red = 1f, green = 0f, blue = 0f)
                R.id.rb_green -> drawColor = DrawColor(red = 0f, green = 1f, blue = 0f)
                R.id.rb_blue -> drawColor = DrawColor(red = 0f, green = 0f, blue = 1f)
                R.id.rb_yellow -> drawColor = DrawColor(red = 1f, green = 1f, blue = 0f)
            }
        }

        binding.btnErase.setOnClickListener {
            currentPoint.forEach {
                GLES20.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
                GLES20.glScissor(it.first, height - it.second, 15, 15)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            }
            currentPoint.clear()
        }

        binding.tbVideoDoodle.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.tbVideoDoodle.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_video_selection_completion -> {
                    saveCompleted(circularEncoder.saveVideo(outputVideo))
                    true
                }
                else -> false
            }
        }
    }

    private fun drawLine(x: Int, y: Int) {
        currentPoint.add(Triple(x, y, drawColor))
    }

    private fun fillSpace(x: Int, y: Int) {
        val last = currentPoint.last()
        for (i in 1..50) {
            currentPoint.add(
                Triple(
                    last.first + (x - last.first) * i / 100,
                    last.second + (y - last.second) * i / 100,
                    drawColor
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
        mediaPlayer.pause()
        binding.btnPlay.setBackgroundDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_baseline_play_arrow_24
            )
        )
        isPlaying = false
    }

    override fun onDestroy() {
        super.onDestroy()
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
        isSurfaceDestroyed = false
//        EGL 설정
        if (eglCore == null) {
            eglCore = EglCore()
            displaySurface = EglWindowSurface(eglCore!!, p0.surface, true)
            displaySurface!!.makeCurrent()

            fullFrameBlit =
                FullFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
            textureId = fullFrameBlit!!.createTextureObject()
            surfaceTexture = SurfaceTexture(textureId)
            surfaceTexture!!.setOnFrameAvailableListener(this)

            val surface = Surface(surfaceTexture)
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
                circularEncoder = Encoder(width, width, 6000000, 30, 60)
            } catch (e: IOException) {
                throw Exception(e)
            }
            encoderSurface = EglWindowSurface(eglCore!!, circularEncoder.inputSurface, true)
        } else {
            displaySurface = EglWindowSurface(eglCore!!, p0.surface, true)
            displaySurface!!.makeCurrent()
            val surface = Surface(surfaceTexture)
            mediaPlayer.setSurface(surface)
        }
    }

    private fun playVideo() {
        if (isPlaying) {
            mediaPlayer.pause()
            binding.btnPlay.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_play_arrow_24
                )
            )
            isPlaying = false
        } else {
            mediaPlayer.start()
//            binding.btnPlay.isEnabled = false
            binding.btnPlay.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_pause_24
                )
            )
            isPlaying = true
        }
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
        if (!isSurfaceDestroyed) drawFrame()
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
            circularEncoder.transferBuffer()
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
            circularEncoder.transferBuffer()
            encoderSurface?.setPresentationTime(surfaceTexture!!.timestamp)
            encoderSurface?.swapBuffers()
            displaySurface?.makeCurrent()
        }
    }

    private fun drawLine(currentPoint: List<Triple<Int, Int, DrawColor>>, height: Int) {
        currentPoint.forEach {
            GLES20.glClearColor(it.third.red, it.third.green, it.third.blue, 1f)
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
            GLES20.glScissor(it.first, height - it.second, 15, 15)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        }
    }

    companion object {
        private const val TAG = "VideoDoodleFragment"
    }
}

data class DrawColor(val red: Float, val green: Float, val blue: Float)