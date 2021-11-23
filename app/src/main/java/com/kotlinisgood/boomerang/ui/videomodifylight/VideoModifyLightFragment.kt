package com.kotlinisgood.boomerang.ui.videomodifylight

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.kotlinisgood.boomerang.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VideoModifyLightFragment : Fragment() {

    private lateinit var binding : FragmentVideoModifyLightBinding
    private val videoModifyLightViewModel: VideoModifyLightViewModel by viewModels()
    private val args: VideoModifyLightFragmentArgs by navArgs()

    private lateinit var viewRecorder: ViewRecorder
    private var recording = false

    private var doodleColor = 0xFFFF0000

    private lateinit var player: ExoPlayer
    private var playerEnded = false

    private var drawView: DrawView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoModifyLightBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewModel()
        setAdapter()
        setListener()
        setBackPressed()
        setObserver()
    }

    fun setViewModel(){
        binding.viewModel = videoModifyLightViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        videoModifyLightViewModel.loadVideoMemo(args.id)
        player = ExoPlayer.Builder(requireContext()).build().apply {
            addListener(playerListener)
            prepare()
        }.also {
            binding.exoplayer.player = it
            binding.pcvVideoModifyLight.player = it
        }

        videoModifyLightViewModel.mediaMemo.observe(viewLifecycleOwner){ videoMemo ->
            val mediaItem = MediaItem.fromUri(videoMemo.mediaUri)
            player.setMediaItem(mediaItem)
        }
    }

    private fun setAdapter(){
        val subVideoAdapter = SubVideoAdapter()
        subVideoAdapter.setOnItemClickListener(object : SubVideoAdapter.OnSubVideoClickListener {
            override fun onItemClick(v: View, position: Int) {
                showSubVideoDialog(position)
            }
        })
        binding.rvSubVideos.adapter = subVideoAdapter
    }

    private fun setDrawingView() {
        drawView = DrawView(requireContext())
        binding.canvas.addView(drawView)
        drawView?.setColor(doodleColor.toInt())
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListener() {
        with(binding) {
            canvas.isEnabled = false
            toggleBtnDoodle.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    val currentTime = player.currentPosition
                    var canMemo = true
                    videoModifyLightViewModel.subVideos.value!!.forEach {
                        if (it.startingTime < currentTime && currentTime < it.endingTime) {
                            canMemo = false
                        }
                    }
                    when {
                        playerEnded || currentTime > player.duration - 300-> {
                            toggleBtnDoodle.uncheck(R.id.btn_doodle)
                            Toast.makeText(context,"영상이 끝났습니다.",Toast.LENGTH_SHORT).show()
                        }
                        canMemo -> {
                            startRecord()
                            binding.canvas.isEnabled = true
                        }
                        else -> {
                            toggleBtnDoodle.uncheck(R.id.btn_doodle)
                            Toast.makeText(context, "이미 메모가 있습니다", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    stopRecord()
                    binding.canvas.isEnabled = false
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

            tbVideoDoodle.throttle(1000,TimeUnit.MILLISECONDS){
                showDialog()
            }

            tbVideoDoodle.menu.forEach{
                when(it.itemId){
                    R.id.menu_video_modify -> {
                        it.throttle(1000, TimeUnit.MILLISECONDS){
                            videoModifyLightViewModel.updateVideoMemo()
                            val action = videoModifyLightViewModel.mediaMemo.value?.let { it1 ->
                                VideoModifyLightFragmentDirections.actionVideoModifyLightFragmentToMemoFragment(
                                    it1.id)
                            }
                            if (action != null) {
                                findNavController().navigate(action)
                            }
                        }
                    }
                }
            }
        }
    }

    fun setObserver(){
        videoModifyLightViewModel.timeOver.observe(viewLifecycleOwner){ timeOver ->
            if(timeOver == true){
                stopRecord()
                videoModifyLightViewModel.resetTimer()
                binding.toggleBtnDoodle.uncheck(R.id.btn_doodle)
                Toast.makeText(context, "영상 시간을 초과하여 메모하실 수 없습니다!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecord() {
        setDrawingView()
        val fileName = System.currentTimeMillis()
        setViewRecorder()
        viewRecorder.setOutputFile(context?.filesDir.toString() + "/$fileName.mp4")
        try {
            viewRecorder.prepare()
            viewRecorder.start()
            videoModifyLightViewModel.setCurrentSubVideo(
                SubVideo(
                Uri.fromFile(File(context?.filesDir, "$fileName.mp4")).toString(),
                player.currentPosition.toInt(),
                player.currentPosition.toInt()
            )
            )
        } catch (e: IOException) {
            Log.e("MainActivity", "startRecord failed", e)
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
            binding.canvas.removeAllViews()
            videoModifyLightViewModel.resetTimer()
            videoModifyLightViewModel.setEndTime(getDuration(File(videoModifyLightViewModel.getCurrentSubVideo()?.uri?.toUri()?.path))!!.toInt())
            recording = false
        }
    }

    private fun getDuration(file: File): String? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(file.absolutePath)
        return mmr.run {
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        }
    }

    private val onErrorListener = MediaRecorder.OnErrorListener { mr, what, extra ->
        Log.e("MainActivity", "MediaRecorder error: type = $what, code = $extra")
        viewRecorder.reset()
        viewRecorder.release()
    }

    private fun setViewRecorder() {
        viewRecorder = ViewRecorder().apply {
            val width = Math.round(binding.canvas.width.toFloat() / 10) * 10
            val height = Math.round(binding.canvas.height.toFloat() / 10) * 10
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoFrameRate(50)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoEncodingBitRate(2000 * 1000)
            setOnErrorListener(onErrorListener)
            setRecordedView(binding.canvas)
        }
    }

    private fun showDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("메모 수정을 중단하시겠습니까?")
            .setMessage("수정 내역은 삭제됩니다.")
            .setNegativeButton("취소") { dialog, which ->
                dialog.dismiss()
            }
            .setPositiveButton("나가기") { dialog, which ->
                val action = VideoModifyLightFragmentDirections.actionVideoModifyLightFragmentToMemoFragment(videoModifyLightViewModel.mediaMemo.value!!.id)
                findNavController().navigate(action)
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
                videoModifyLightViewModel.deleteSubVideo(position)
            }
            .show()
    }

    private fun setBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){
            showDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.toggleBtnDoodle.uncheck(R.id.btn_doodle)
        stopRecord()
        player.run {
            pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.run{
            stop()
            removeListener(playerListener)
            release()
        }
    }

    private val playerListener = object: Player.Listener{
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when(playbackState){
                Player.STATE_ENDED -> {
                    playerEnded = true
                    if(recording){
                        stopRecord()
                        binding.toggleBtnDoodle.uncheck(R.id.btn_doodle)
                    }
                }
                Player.STATE_READY -> {
                    videoModifyLightViewModel.setDuration(player.duration)
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying){
                playerEnded = false
            }
        }
    }
}