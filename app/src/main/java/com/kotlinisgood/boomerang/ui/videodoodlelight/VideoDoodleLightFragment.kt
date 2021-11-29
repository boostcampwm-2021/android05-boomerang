package com.kotlinisgood.boomerang.ui.videodoodlelight

import android.annotation.SuppressLint
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoDoodleLightBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.util.ViewRecorder
import com.kotlinisgood.boomerang.util.Util
import com.kotlinisgood.boomerang.util.Util.showToast
import com.kotlinisgood.boomerang.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VideoDoodleLightFragment : Fragment() {

    private var _dataBinding: FragmentVideoDoodleLightBinding? = null
    private val dataBinding get() = _dataBinding!!

    private val videoDoodleLightViewModel: VideoDoodleLightViewModel by viewModels()

    private val args: VideoDoodleLightFragmentArgs by navArgs()
    private lateinit var uriString: String
    private val uri: Uri get() = uriString.toUri()

    private lateinit var viewRecorder: ViewRecorder
    private lateinit var player: ExoPlayer

    private var recording = false
    private var playerEnded = false

    private var drawView: DrawView? = null
    private var doodleColor = 0xFFFF0000

    private val compositeDisposable by lazy { CompositeDisposable() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_video_doodle_light,
            container,
            false
        )
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uriString = args.videoPath
        setViewModel()
        setAdapter()
        setVideoView()
        setListener()
        setObserver()
        setBackPressed()
    }

    private fun setViewModel() {
        dataBinding.viewModel = videoDoodleLightViewModel
        dataBinding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setAdapter() {
        val subVideoAdapter = SubVideoAdapter()
        subVideoAdapter.setOnItemClickListener(object : SubVideoAdapter.OnSubVideoClickListener {
            override fun onItemClick(v: View, position: Int) {
                showSubVideoDialog(position)
            }
        })
        dataBinding.rvSubVideos.adapter = subVideoAdapter
    }

    private fun setVideoView() {
        val mediaItem = MediaItem.fromUri(uri)
        player = ExoPlayer.Builder(requireContext()).build()
        dataBinding.exoplayer.player = player
        dataBinding.pcvVideoDoodleLight.player = player
        player.setMediaItem(mediaItem)
        player.addListener(playerListener)
        player.prepare()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListener() {
        with(dataBinding) {
            canvas.isEnabled = false
            toggleBtnDoodle.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    val currentTime = player.currentPosition
                    var canMemo = true
                    videoDoodleLightViewModel.subVideos.value!!.forEach {
                        if (it.startingTime <= currentTime && currentTime <= it.endingTime) {
                            canMemo = false
                        }
                    }
                    when {
                        playerEnded || currentTime > player.duration - 300 -> {
                            toggleBtnDoodle.uncheck(R.id.btn_doodle)
                            this@VideoDoodleLightFragment.showToast("영상이 끝났습니다")
                        }
                        canMemo -> {
                            startRecord()
                            dataBinding.canvas.isEnabled = true
                        }
                        else -> {
                            toggleBtnDoodle.uncheck(R.id.btn_doodle)
                            this@VideoDoodleLightFragment.showToast("이미 메모가 있습니다")
                        }
                    }

                } else {
                    stopRecord()
                    dataBinding.canvas.isEnabled = false
                }
            }

            rbRed.isChecked = true
            rgDoodleColor.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.rb_red -> doodleColor = 0xFFFF0000
                    R.id.rb_green -> doodleColor = 0xFF00FF00
                    R.id.rb_blue -> doodleColor = 0xFF0000FF
                    R.id.rb_yellow -> doodleColor = 0xFFFFFF00
                }
                drawView?.setColor(doodleColor.toInt())
            }

            compositeDisposable.add(tbVideoDoodle.throttle(1000, TimeUnit.MILLISECONDS) {
                showDialog()
            })

            tbVideoDoodle.menu.forEach {
                when (it.itemId) {
                    R.id.menu_video_doodle -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) {
                            dataBinding.canvas.isEnabled = false
                            stopRecord()
                            val action =
                                VideoDoodleLightFragmentDirections.actionVideoDoodleLightFragmentToVideoEditLightFragment(
                                    uriString,
                                    videoDoodleLightViewModel.subVideos.value!!.toTypedArray(),
                                    false
                                )
                            findNavController().navigate(action)
                        })
                    }
                }
            }
        }
    }

    private fun setObserver() {
        videoDoodleLightViewModel.timeOver.observe(viewLifecycleOwner) { timeOver ->
            if (timeOver == true) {
                stopRecord()
                videoDoodleLightViewModel.resetTimer()
                dataBinding.toggleBtnDoodle.uncheck(R.id.btn_doodle)
                this.showToast("영상 시간을 초과하여 메모하실 수 없습니다")
            }
        }
    }

    private fun setBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showDialog()
        }
    }

    private fun setDrawingView() {
        drawView = DrawView(requireContext())
        dataBinding.canvas.addView(drawView)
        drawView?.setColor(doodleColor.toInt())
    }

    private fun startRecord() {
        val fileName = System.currentTimeMillis()
        val filePath = requireActivity().filesDir.absolutePath + "/$fileName.mp4"
        setDrawingView()
        setViewRecorder()
        viewRecorder.setOutputFile(filePath)
        try {
            viewRecorder.prepare()
            viewRecorder.start()
            videoDoodleLightViewModel.setCurrentSubVideo(
                SubVideo(
                    filePath,
                    player.currentPosition.toInt(),
                    player.currentPosition.toInt()
                )
            )
        } catch (e: IOException) {
            Log.e("MainActivity", "startRecord failed", e)
            return
        }
        videoDoodleLightViewModel.startRecordTime()
        recording = true
    }

    private fun stopRecord() {
        if (recording) {
            viewRecorder.stop()
            viewRecorder.reset()
            viewRecorder.release()
            dataBinding.canvas.removeAllViews()
            videoDoodleLightViewModel.resetTimer()
            videoDoodleLightViewModel.setEndTime(
                Util.getDuration(
                    File(
                        videoDoodleLightViewModel.getCurrentSubVideo()
                        !!.uri.toUri().path!!
                    )
                )!!.toInt()
            )
            recording = false
        }
    }

    private fun setViewRecorder() {
        viewRecorder = ViewRecorder().apply {
            val width = Math.round(dataBinding.canvas.width.toFloat() / 10) * 10
            val height = Math.round(dataBinding.canvas.height.toFloat() / 10) * 10
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoFrameRate(50)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoEncodingBitRate(2000 * 1000)
            setOnErrorListener(onErrorListener)
            setRecordedView(dataBinding.canvas)
        }
    }

    private val onErrorListener = MediaRecorder.OnErrorListener { mr, what, extra ->
        Log.e("MainActivity", "MediaRecorder error: type = $what, code = $extra")
        viewRecorder.reset()
        viewRecorder.release()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_ENDED -> {
                    playerEnded = true
                    if (recording) {
                        stopRecord()
                        dataBinding.toggleBtnDoodle.uncheck(R.id.btn_doodle)
                    }
                }
                Player.STATE_READY -> {
                    playerEnded = false
                    videoDoodleLightViewModel.setDuration(player.duration)
                }

            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying) {
                playerEnded = false
            }
        }
    }

    private fun showDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("메모 작성을 중단하시겠습니까?")
            .setMessage("작성 중인 메모는 삭제됩니다.")
            .setNegativeButton("취소") { dialog, which ->
                dialog.dismiss()
            }
            .setPositiveButton("삭제") { dialog, which ->
                videoDoodleLightViewModel.subVideos.value!!.forEach {
                    val file = File(it.uri.toUri().path!!)
                    file.delete()
                }
                findNavController().popBackStack()
            }
            .show()
    }

    private fun showSubVideoDialog(position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("해당 메모를 삭제하시겠습니까?")
            .setMessage("삭제한 메모는 되돌릴 수 없습니다.")
            .setNegativeButton("취소") { dialog, which ->
                dialog.dismiss()
            }
            .setPositiveButton("삭제") { dialog, which ->
                videoDoodleLightViewModel.deleteSubVideo(position)
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        dataBinding.toggleBtnDoodle.uncheck(R.id.btn_doodle)
        stopRecord()
        player.run {
            pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.run {
            removeListener(playerListener)
            release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        _dataBinding = null
    }
}
