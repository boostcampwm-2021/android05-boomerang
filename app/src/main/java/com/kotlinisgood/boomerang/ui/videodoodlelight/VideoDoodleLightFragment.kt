package com.kotlinisgood.boomerang.ui.videodoodlelight

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoDoodleLightBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.util.ViewRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class VideoDoodleLightFragment : Fragment() {

    private lateinit var binding: FragmentVideoDoodleLightBinding
    private val args: VideoDoodleLightFragmentArgs by navArgs()

    private lateinit var viewRecorder: ViewRecorder
    private var recording = false

    private lateinit var uri: Uri
    private lateinit var path: String
    private val subVideos: MutableList<SubVideo> = mutableListOf()

    private var doodleColor = 0xFFFF0000

    private lateinit var seekBar: SeekBar
    private lateinit var job : Job

    private var drawView: DrawView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater,R.layout.fragment_video_doodle_light,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        path = args.videoPath
        uri = path.toUri()
        setVideoView()
        setListener()
    }

    private fun setDrawingView() {
        drawView = DrawView(requireContext())
        binding.canvas.addView(drawView)
        drawView?.setColor(doodleColor.toInt())
    }

    private fun setVideoView(){
        val file = File(path)
        binding.videoView.setVideoPath(file.absolutePath)
        seekBar = binding.sbVideoTimeline
        binding.videoView.setOnPreparedListener {
            seekBar.max = binding.videoView.duration/1000
            job = CoroutineScope(Dispatchers.Default).launch {
                while(true) {
                    seekBar.progress = binding.videoView.currentPosition/1000
                }
            }
        }
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

        })
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setListener() {
        with(binding){
            canvas.isEnabled = false
            btnMemoStart.setOnClickListener {
                if(recording){
                    stopRecord()
                    binding.canvas.isEnabled = false
                } else {
                    startRecord()
                    binding.canvas.isEnabled = true
                }
            }
            btnMoveToResult.setOnClickListener {
                binding.canvas.isEnabled = false
                stopRecord()
                val action = VideoDoodleLightFragmentDirections.actionVideoDoodleLightFragmentToVideoEditLightFragment(subVideos.toTypedArray())
                findNavController().navigate(action)
            }
            btnVideoPlay.setOnClickListener {
                videoView.start()
            }
            btnVideoPause.setOnClickListener {
                videoView.pause()
            }
            rbRed.isChecked = true
            rgDoodleColor.setOnCheckedChangeListener { group, checkedId ->
                when(checkedId){
                    R.id.rb_red -> doodleColor = 0xFFFF0000
                    R.id.rb_green -> doodleColor = 0xFF00FF00
                    R.id.rb_blue -> doodleColor = 0xFF0000FF
                    R.id.rb_yellow -> doodleColor = 0xFFFFFF00
                }
                drawView?.setColor(doodleColor.toInt())
            }
        }
    }

    private fun startRecord() {
        setDrawingView()
        val fileName = System.currentTimeMillis()
        viewRecorder = ViewRecorder().apply {
            val width = Math.round(binding.canvas.width.toFloat()/10)*10
            val height = Math.round(binding.canvas.height.toFloat()/10)*10
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoFrameRate(50)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoEncodingBitRate(2000 * 1000)
            setOutputFile(context?.cacheDir.toString() + "/$fileName.mp4")
            setOnErrorListener(onErrorListener)
            setRecordedView(binding.canvas)
        }
        try {
            viewRecorder.prepare()
            viewRecorder.start()
            subVideos.add(SubVideo(Uri.fromFile(File(context?.cacheDir, "$fileName.mp4")),binding.videoView.currentPosition,binding.videoView.currentPosition))
        } catch (e: IOException) {
            Log.e("MainActivity", "startRecord failed", e)
            return
        }
        Log.d("MainActivity", "startRecord successfully!")
        recording = true
    }

    private fun stopRecord() {
        if (recording) {
            viewRecorder.stop()
            viewRecorder.reset()
            viewRecorder.release()
            binding.canvas.removeAllViews()
            subVideos.last().endingTime = binding.videoView.currentPosition
            Log.d("MainActivity", "stopRecord successfully!")
            recording = false
            println(subVideos)
        }
    }

    private val onErrorListener = MediaRecorder.OnErrorListener { mr, what, extra ->
        Log.e("MainActivity", "MediaRecorder error: type = $what, code = $extra")
        viewRecorder.reset()
        viewRecorder.release()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job.cancel()
    }
}