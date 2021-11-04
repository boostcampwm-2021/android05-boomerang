package com.kotlinisgood.boomerang.ui.videoeditlight

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoEditLightBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import kotlinx.coroutines.*

class VideoEditLightFragment : Fragment() {

    private lateinit var binding: FragmentVideoEditLightBinding

    private val args: VideoEditLightFragmentArgs by navArgs()
    private lateinit var subVideos: MutableList<SubVideo>
    private var isPlayings = mutableListOf<Boolean>()

    private lateinit var player: SimpleExoPlayer
    private lateinit var baseUri: Uri

    lateinit var job: Job
    private var isPlaying = false
    private var currentTime = 0L

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
        setCurrentTime()
        setScanner()
        setPlayer()
    }

    private fun setVideoView() {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        binding.exoplayer.player = player
        baseUri = args.baseVideo.toUri()
        val mediaItem = MediaItem.fromUri(baseUri)
        player.setMediaItem(mediaItem)
        var string = ""
        subVideos.forEach {
            string += "$it\n"
        }
        binding.tvSubvideos.text = string
    }

    private fun setPlayer() {
        player.addListener(onPlayStateChangeListener)
    }

    private fun setCurrentTime() {
        lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                if (isPlaying) {
                    withContext(Dispatchers.Main) {
                        currentTime = player.currentPosition
                    }
                    delay(500)
                }
            }
        }
    }

    private fun setScanner() {
        repeat(subVideos.size) { isPlayings.add(false) }
        job = lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                if (isPlaying) {
                    subVideos.forEachIndexed { index, subVideo ->
                        val time = currentTime
                        // 실행시켜야되는지?
                        if ((subVideo.startingTime / 1000).toLong() == time / 1000 && !isPlayings[index]) {
                            withContext(Dispatchers.Main) {
                                isPlayings[index] = true
                                binding.alphaView.setVideoFromUri(context, subVideo.uri)
                                binding.alphaView.mediaPlayer.setOnPreparedListener { mp ->
                                    binding.alphaView.visibility = View.VISIBLE
                                    binding.alphaView.mediaPlayer.start()
                                    binding.alphaView.setLooping(false)
                                }
                                binding.alphaView.setOnVideoEndedListener {
                                    isPlayings[index] = false
                                    binding.alphaView.visibility = View.GONE
                                    binding.alphaView.seekTo(0)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val onPlayStateChangeListener = object : Player.Listener {
        override fun onIsPlayingChanged(flag: Boolean) {
            isPlaying = flag
        }
    }
}