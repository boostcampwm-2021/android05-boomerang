package com.kotlinisgood.boomerang.ui.videomodifylight

import android.annotation.SuppressLint
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoModifyLightBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.DrawView
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideoAdapter
import com.kotlinisgood.boomerang.ui.videodoodlelight.util.ViewRecorder
import com.kotlinisgood.boomerang.util.Util
import com.kotlinisgood.boomerang.util.Util.showSnackBar
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import com.kotlinisgood.boomerang.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VideoModifyLightFragment : Fragment() {

    private var _dataBinding: FragmentVideoModifyLightBinding? = null
    private val dataBinding get() = _dataBinding!!

    private val videoModifyLightViewModel: VideoModifyLightViewModel by viewModels()

    private val args: VideoModifyLightFragmentArgs by navArgs()

    private lateinit var viewRecorder: ViewRecorder
    private lateinit var player: ExoPlayer

    private var recording = false
    private var playerEnded = false

    private var drawView: DrawView? = null
    private var doodleColor = 0xFFFF0000

    private val compositeDisposable by lazy { CompositeDisposable() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding = FragmentVideoModifyLightBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewModel()
        setAdapter()
        setVideoView()
        setListener()
        setObserver()
        setBackPressed()
    }

    private fun setViewModel() {
        dataBinding.viewModel = videoModifyLightViewModel
        dataBinding.lifecycleOwner = viewLifecycleOwner
        videoModifyLightViewModel.loadVideoMemo(args.id)
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
        player = ExoPlayer.Builder(requireContext()).build().apply {
            addListener(playerListener)
            prepare()
        }.also {
            dataBinding.exoplayerVideoModify.player = it
            dataBinding.pcvVideoModifyLight.player = it
        }
        videoModifyLightViewModel.mediaMemo.observe(viewLifecycleOwner) { videoMemo ->
            val mediaItem = MediaItem.fromUri(videoMemo.mediaUri)
            player.setMediaItem(mediaItem)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListener() {
        with(dataBinding) {
            containerAlphaView.isEnabled = false
            toggleBtnDoodle.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    val currentTime = player.currentPosition
                    var canMemo = true
                    videoModifyLightViewModel.subVideos.value!!.forEach {
                        if (it.startingTime <= currentTime && currentTime <= it.endingTime) {
                            canMemo = false
                        }
                    }
                    when {
                        playerEnded || currentTime > player.duration - 300 -> {
                            toggleBtnDoodle.uncheck(R.id.btn_doodle)
                            dataBinding.containerVideoModifyLight.showSnackBar(
                                getString(R.string.snackbar_video_doodle_light_video_end)
                            )
                        }
                        canMemo -> {
                            startRecord()
                            dataBinding.containerAlphaView.isEnabled = true
                        }
                        else -> {
                            toggleBtnDoodle.uncheck(R.id.btn_doodle)
                            dataBinding.containerVideoModifyLight.showSnackBar(
                                getString(R.string.snackbar_video_doodle_light_cant_memo)
                            )
                        }
                    }

                } else {
                    stopRecord()
                    dataBinding.containerAlphaView.isEnabled = false
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
                    R.id.menu_video_modify -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) {
                            videoModifyLightViewModel.updateVideoMemo()
                            val action =
                                videoModifyLightViewModel.mediaMemo.value?.let { mediaMemo ->
                                    VideoModifyLightFragmentDirections.actionVideoModifyLightFragmentToMemoFragment(
                                        mediaMemo.id, VIDEO_MODE_SUB_VIDEO
                                    )
                                }
                            if (action != null) {
                                findNavController().navigate(action)
                            }
                        })
                    }
                }
            }
        }
    }

    private fun setObserver() {
        videoModifyLightViewModel.timeOver.observe(viewLifecycleOwner) { timeOver ->
            if (timeOver == true) {
                stopRecord()
                videoModifyLightViewModel.resetTimer()
                dataBinding.toggleBtnDoodle.uncheck(R.id.btn_doodle)
                dataBinding.containerVideoModifyLight.showSnackBar(getString(R.string.snackbar_video_doodle_light_video_over))
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
        dataBinding.containerAlphaView.addView(drawView)
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
            videoModifyLightViewModel.setCurrentSubVideo(
                SubVideo(
                    filePath,
                    player.currentPosition.toInt(),
                    player.currentPosition.toInt()
                )
            )
        } catch (e: IOException) {
            return
        }
        videoModifyLightViewModel.startRecordTime()
        recording = true
    }

    private fun stopRecord() {
        if (recording) {
            viewRecorder.stop()
            viewRecorder.reset()
            viewRecorder.release()
            dataBinding.containerAlphaView.removeAllViews()
            videoModifyLightViewModel.resetTimer()
            videoModifyLightViewModel.setEndTime(
                Util.getDuration(File(videoModifyLightViewModel.getCurrentSubVideo()?.uri?.toUri()?.path!!))!!
                    .toInt()
            )
            recording = false
        }
    }

    private val onErrorListener = MediaRecorder.OnErrorListener { _, _, _ ->
        viewRecorder.reset()
        viewRecorder.release()
    }

    private fun setViewRecorder() {
        viewRecorder = ViewRecorder().apply {
            val width = Math.round(dataBinding.containerAlphaView.width.toFloat() / 10) * 10
            val height = Math.round(dataBinding.containerAlphaView.height.toFloat() / 10) * 10
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoFrameRate(50)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoEncodingBitRate(2000 * 1000)
            setOnErrorListener(onErrorListener)
            setRecordedView(dataBinding.containerAlphaView)
        }
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
                    videoModifyLightViewModel.setDuration(player.duration)
                }
                Player.STATE_BUFFERING -> {}
                Player.STATE_IDLE -> {}
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
            .setTitle(getString(R.string.dialog_title_video_modify))
            .setMessage(getString(R.string.dialog_message_video_modify))
            .setNegativeButton(getString(R.string.dialog_negative_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.dialog_positive_out)) { _, _ ->
                val action =
                    VideoModifyLightFragmentDirections.actionVideoModifyLightFragmentToMemoFragment(
                        videoModifyLightViewModel.mediaMemo.value!!.id,
                        VIDEO_MODE_SUB_VIDEO
                    )
                findNavController().navigate(action)
            }
            .show()
    }

    private fun showSubVideoDialog(position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_video_doodle_light_delete_subvideo))
            .setMessage(getString(R.string.dialog_message_video_doodle_light_delete_subvideo))
            .setNegativeButton(R.string.dialog_negative_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.dialog_positive_delete) { _, _ ->
                videoModifyLightViewModel.deleteSubVideo(position)
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
            stop()
            removeListener(playerListener)
            release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
        compositeDisposable.dispose()
    }
}