package com.kotlinisgood.boomerang.ui.videodoodlelight

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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoDoodleLightBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.util.ViewRecorder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException

@AndroidEntryPoint
class VideoDoodleLightFragment : Fragment() {

    private lateinit var binding: FragmentVideoDoodleLightBinding
    private val args: VideoDoodleLightFragmentArgs by navArgs()
    private val viewModel: VideoDoodleLightViewModel by viewModels()

    private lateinit var viewRecorder: ViewRecorder
    private var recording = false

    private lateinit var uri: Uri
    private lateinit var path: String

    private var doodleColor = 0xFFFF0000

    private lateinit var player: SimpleExoPlayer

    private var drawView: DrawView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_video_doodle_light,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        path = args.videoPath
        uri = path.toUri()
        setVideoView()
        setListener()
        setBackPressed()
        setViewModel()
        setAdapter()
    }

    private fun setViewModel() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setAdapter() {
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

    private fun setVideoView() {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        binding.exoplayer.player = player
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
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
                binding.canvas.isEnabled = false
                stopRecord()
                val action =
                    VideoDoodleLightFragmentDirections.actionVideoDoodleLightFragmentToVideoEditLightFragment(
                        path,
                        viewModel!!.subVideos.value!!.toTypedArray()
                    )
                findNavController().navigate(action)
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
            .setTitle("메모 작성을 중단하시겠습니까?")
            .setMessage("작성 중인 메모는 삭제됩니다.")
            .setNegativeButton("취소") { dialog, which ->
                dialog.dismiss()
            }
            .setPositiveButton("삭제") { dialog, which ->
                viewModel.subVideos.value!!.forEach {
                    val file = File(it.uri.toUri().path)
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
                viewModel.deleteSubVideo(position)
            }
            .show()
    }

    private fun setBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        player.run {
            stop()
            release()
        }
    }
}
