package com.kotlinisgood.boomerang.ui.videoeditlight

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoEditLightBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class VideoEditLightFragment : Fragment() {

    private lateinit var binding: FragmentVideoEditLightBinding

    private val args: VideoEditLightFragmentArgs by navArgs()
    private lateinit var subVideos: MutableList<SubVideo>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_video_edit_light,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subVideos = args.subVideos.toMutableList()
        setVideoView()
        setListener()
    }

    private fun setVideoView() {
        val file = File(context?.cacheDir, "sample.mp4")
        binding.videoView.setVideoPath(file.absolutePath)
    }

    private fun setListener() {
        binding.btnVideoStart.setOnClickListener {
            with(binding) {
                videoView.start()
                CoroutineScope(Dispatchers.Main).launch {
                    subVideos.forEachIndexed { index, subVideo ->
                        if (index == 0) {
                            delay(subVideo.startingTime.toLong())
                        } else {
                            delay((subVideo.startingTime - subVideos[index - 1].startingTime).toLong())
                        }
                        alphaView.setVideoFromUri(context, subVideo.uri)
                        alphaView.mediaPlayer.setOnPreparedListener {
                            alphaView.mediaPlayer.start()
                            alphaView.visibility = View.VISIBLE
                            alphaView.setLooping(false)
                            alphaView.setOnVideoEndedListener {
                                alphaView.visibility = View.GONE
                                alphaView.seekTo(0)
                            }
                        }
                    }
                }
            }
        }
    }
}