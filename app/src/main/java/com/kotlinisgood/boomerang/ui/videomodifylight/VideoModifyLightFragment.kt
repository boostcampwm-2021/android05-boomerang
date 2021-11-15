package com.kotlinisgood.boomerang.ui.videomodifylight

import android.annotation.SuppressLint
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoModifyLightBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.DrawView
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideoAdapter
import com.kotlinisgood.boomerang.ui.videodoodlelight.VideoDoodleLightFragmentDirections
import com.kotlinisgood.boomerang.ui.videodoodlelight.util.ViewRecorder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException

@AndroidEntryPoint
class VideoModifyLightFragment : Fragment() {

    private lateinit var binding : FragmentVideoModifyLightBinding
    private val viewModel: VideoModifyLightViewModel by viewModels()
    private val args: VideoModifyLightFragmentArgs by navArgs()

    private lateinit var viewRecorder: ViewRecorder
    private var recording = false

    private var doodleColor = 0xFFFF0000

    private lateinit var player: SimpleExoPlayer

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
    }

    fun setViewModel(){
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.loadVideoMemo(args.id)
        player = SimpleExoPlayer.Builder(requireContext()).build()
        binding.exoplayer.player = player
        viewModel.videoMemo.observe(viewLifecycleOwner){ videoMemo ->
            val mediaItem = MediaItem.fromUri(videoMemo.videoUri)
            player.setMediaItem(mediaItem)
        }
    }

    private fun setAdapter(){
        binding.rvSubVideos.adapter = SubVideoAdapter()
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
                    viewModel!!.subVideos.value!!.forEach {
                        if (it.startingTime < currentTime && currentTime < it.endingTime) {
                            canMemo = false
                        }
                    }
                    if (canMemo) {
                        startRecord()
                        binding.canvas.isEnabled = true
                    } else {
                        toggleBtnDoodle.uncheck(R.id.btn_doodle)
                        Toast.makeText(context, "이미 메모가 있습니다", Toast.LENGTH_SHORT).show()
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

            btnMoveToResult.setOnClickListener {
                viewModel!!.updateVideoMemo()
                val action = viewModel!!.videoMemo.value?.let { it1 ->
                    VideoModifyLightFragmentDirections.actionVideoModifyLightFragmentToMemoFragment(
                        it1.id)
                }
                if (action != null) {
                    findNavController().navigate(action)
                }
            }

            btnGoBack.setOnClickListener {
                showDialog()
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
            viewModel.setCurrentSubVideo(
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
        recording = true
    }

    private fun stopRecord() {
        if (recording) {
            viewRecorder.stop()
            viewRecorder.reset()
            viewRecorder.release()
            binding.canvas.removeAllViews()
            viewModel.setEndTime(player.currentPosition.toInt())
            recording = false
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
                findNavController().popBackStack()
            }
            .show()
    }

    private fun setBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){
            showDialog()
        }
    }
}