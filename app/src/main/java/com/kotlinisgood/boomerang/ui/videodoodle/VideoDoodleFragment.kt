package com.kotlinisgood.boomerang.ui.videodoodle

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLES30
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoDoodleBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.util.throttle
import com.kotlinisgood.boomerang.util.throttle1000
import com.kotlinisgood.boomerang.util.throttle500
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.floor


class VideoDoodleFragment : Fragment(), SurfaceHolder.Callback,
    SurfaceTexture.OnFrameAvailableListener {

    private var _dataBinding: FragmentVideoDoodleBinding? = null
    private val dataBinding get() = _dataBinding!!

    private val videoDoodleViewModel: VideoDoodleViewModel by viewModels()

    private val args: VideoDoodleFragmentArgs by navArgs()
    private val uriString by lazy { args.videoPath }

    private lateinit var egl: Egl
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var displaySurface: EglWindowSurface
    private lateinit var surfaceView: SurfaceView

    private lateinit var encoderSurface: EglWindowSurface
    private var textureId = 0
    private lateinit var fullFrameBlit: FullFrameRect
    private val mTmpMatrix = FloatArray(16)
//    private lateinit var encoder: Encoder

    private lateinit var mediaPlayer: MediaPlayer
    private var videoWidth = 0
    private var videoHeight = 0
    private var width = 0
    private var height = 0
    private lateinit var outputVideo: File

    private var viewportX = 0
    private var viewportY = 0
    private var viewportWidth = 0
    private var viewportHeight = 0

    //    x, y, 색상
    private var currentPoint: MutableList<Triple<Int, Int, DrawColor>> = mutableListOf()
    private var drawColor = DrawColor(red = 1f, green = 0f, blue = 0f)
    private var isPlaying = false
    private var isSurfaceDestroyed = false

    private val compositeDisposable by lazy { CompositeDisposable() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding =
            FragmentVideoDoodleBinding.inflate(inflater, container, false)
//        val mmr = MediaMetadataRetriever()
//        mmr.setDataSource(UriUtil.getPathFromUri(requireContext().contentResolver, uriString.toUri()))
//        val testHeight =
//            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
//                ?.toInt()!!
//        val testWidth = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
//            ?.toInt()!!
//        val ratio = testWidth/testHeight.toDouble()
//        println("Before: $ratio")
//        dataBinding.frameMovie.setAspectRatio("%.4f".format(ratio).toDouble())
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        surfaceView = dataBinding.svMovie
        surfaceView.holder.addCallback(this)

        val currentUnixTime = System.currentTimeMillis()
        outputVideo = File(requireContext().filesDir, "${currentUnixTime}.mp4")

        val uri = uriString.toUri()
        mediaPlayer = MediaPlayer.create(context, uri)

        mediaPlayer.setOnPreparedListener {
            dataBinding.btnPlay.isEnabled = true
        }

//        실수부 값이 너무 커지지 않도록 끊어줘야 함
        val ratio = floor(mediaPlayer.videoWidth.toDouble()/mediaPlayer.videoHeight*10000)/10000.0
//        val ratio = 1.0000
        println("Before: $ratio")
//        dataBinding.frameMovie.setAspectRatio("%.4f".format(ratio).toDouble())
        dataBinding.frameMovie.setAspectRatio(ratio)

        compositeDisposable.add(dataBinding.btnPlay.throttle(throttle1000, TimeUnit.MILLISECONDS) {
            playVideo()
        })

        dataBinding.frameMovie.setOnTouchListener { _, motionEvent ->
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
            dataBinding.frameMovie.performClick()
            true
        }

        dataBinding.rbRed.isChecked = true
        dataBinding.rgDoodleColor.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_red -> drawColor = DrawColor(red = 1f, green = 0f, blue = 0f)
                R.id.rb_green -> drawColor = DrawColor(red = 0f, green = 1f, blue = 0f)
                R.id.rb_blue -> drawColor = DrawColor(red = 0f, green = 0f, blue = 1f)
                R.id.rb_yellow -> drawColor = DrawColor(red = 1f, green = 1f, blue = 0f)
            }
        }

        compositeDisposable.add(dataBinding.btnErase.throttle(throttle500, TimeUnit.MILLISECONDS) {
            currentPoint.forEach {
                GLES20.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
                GLES20.glScissor(it.first, height - it.second, 15, 15)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            }
            currentPoint.clear()
        })

        compositeDisposable.add(dataBinding.tbVideoDoodle.throttle(throttle1000, TimeUnit.MILLISECONDS) {
            findNavController().popBackStack()
        })

        dataBinding.tbVideoDoodle.menu.forEach {
            when (it.itemId) {
                R.id.menu_video_selection_completion -> {
                    compositeDisposable.add(it.throttle(throttle1000, TimeUnit.MILLISECONDS) {
                        videoDoodleViewModel.encoder.muxerStop()
                        saveCompleted()
                    })
                }
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

    private fun saveCompleted() {
        Log.d(TAG, "Save Completed")
        val action =
            VideoDoodleFragmentDirections.actionVideoDoodleFragmentToVideoEditLightFragment(
                outputVideo.toUri().toString(),
                mutableListOf<SubVideo>().toTypedArray(),
                true
            )
        findNavController().navigate(action)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
        dataBinding.btnPlay.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_doodle_play
        )
        isPlaying = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer.release()
        surfaceTexture.release()
//        viewModel.encoder.shutdown()
        encoderSurface.release()
        displaySurface.release()
        fullFrameBlit.release(false)
        egl.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        _dataBinding = null
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated: surfaceHolder=$p0")
        isSurfaceDestroyed = false
        egl = Egl()
        displaySurface = EglWindowSurface(egl, p0.surface)
        displaySurface.makeCurrent()

        fullFrameBlit = FullFrameRect()
        textureId = fullFrameBlit.createTextureObject()
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(this)

        val surface = Surface(surfaceTexture)
        mediaPlayer.setSurface(surface)
        videoWidth = mediaPlayer.videoWidth
        videoHeight = mediaPlayer.videoHeight

        width = dataBinding.svMovie.width
        height = dataBinding.svMovie.height

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

        if (!videoDoodleViewModel.isEncoderWorking) {
            videoDoodleViewModel.encoder = Encoder(width, height, 6000000, 30, outputVideo)
            encoderSurface = EglWindowSurface(egl, videoDoodleViewModel.encoder.inputSurface)
        }
        videoDoodleViewModel.isEncoderWorking = true
    }

    private fun playVideo() {
        if (isPlaying) {
            mediaPlayer.pause()
            dataBinding.btnPlay.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_doodle_play
            )
            isPlaying = false
        } else {
            mediaPlayer.start()
            dataBinding.btnPlay.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_doodle_pause
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
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(mTmpMatrix)

//        SurfaceView에 그리기
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        fullFrameBlit.drawFrame(textureId, mTmpMatrix)
        drawLine(currentPoint, height)

        if (egl.glVersion == 3) {
//        SurfaceView에 그릴 Framebuffer를 아직 swap하지 말고 인코딩 버퍼에 복사하고 둘 다 swap
            encoderSurface.makeCurrentReadFrom(displaySurface)
            GLES30.glBlitFramebuffer(
                0,
                0,
                displaySurface.width,
                displaySurface.height,
                0,
                0,
                displaySurface.width,
                displaySurface.height,
                GLES30.GL_COLOR_BUFFER_BIT,
                GLES30.GL_NEAREST
            )
            videoDoodleViewModel.encoder.transferBuffer()
            encoderSurface.setPresentationTime(surfaceTexture.timestamp)
            encoderSurface.swapBuffers()

            displaySurface.makeCurrent()
            displaySurface.swapBuffers()
        } else {
//            OpenGL ES 2.0일 경우. glBlitFramebuffer를 지원하지 않기에 그냥 두 번 그린다.
            displaySurface.swapBuffers()

            encoderSurface.makeCurrent()
            GLES20.glViewport(0, 0, width, height)
            fullFrameBlit.drawFrame(textureId, mTmpMatrix)
            drawLine(currentPoint, height)
            videoDoodleViewModel.encoder.transferBuffer()
            encoderSurface.setPresentationTime(surfaceTexture.timestamp)
            encoderSurface.swapBuffers()
            displaySurface.makeCurrent()
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