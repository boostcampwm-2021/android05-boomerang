package com.kotlinisgood.boomerang.ui.videoedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoEditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class VideoEditFragment : Fragment() {

    private lateinit var binding: FragmentVideoEditBinding
    private val viewModel: VideoEditViewModel by viewModels()
    private val args: VideoEditFragmentArgs by navArgs()

    private var isPlayings = mutableListOf<Boolean>()

    private lateinit var player: SimpleExoPlayer

    lateinit var jobScanner: Job
    lateinit var jobTimer: Job

    @Volatile
    private var isPlaying = false

    @Volatile
    private var currentTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_video_edit,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewModel()
        setVideoView()
        setScanner()
        setPlayer()
        setListener()
    }

    private fun setViewModel() {
        viewModel.setSubVideo(args.subVideos.toMutableList())
        viewModel.setVideoUri(args.baseVideo.toUri())
    }

    private fun setListener() {
        binding.etMemoTitle.doOnTextChanged { text, start, before, count ->
            viewModel.setTitle(text.toString())
        }

        binding.btnSaveMemo.setOnClickListener {
            viewModel.saveMemo()
            findNavController().navigate(R.id.action_videoEditLightFragment_to_homeFragment)
        }
    }

    private fun setVideoView() {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        binding.exoplayer.player = player
        val mediaItem = MediaItem.fromUri(viewModel.getVideoUri())
        player.setMediaItem(mediaItem)
        var string = ""
        viewModel.getSubVideo().forEach {
            string += "$it\n"
        }
        binding.tvSubvideos.text = string
    }

    private fun setPlayer() {
        player.addListener(onPlayStateChangeListener)
    }

    private suspend fun timer() {
        withContext(Dispatchers.Default) {
            while (true) {
                if (isPlaying) {
                    withContext(Dispatchers.Main) {
                        currentTime = player.currentPosition
                    }
                }
                delay(500)
            }
        }
    }

    private suspend fun scan() {
        withContext(Dispatchers.Default) {
            while (true) {
                if (isPlaying) {
                    viewModel.getSubVideo().forEachIndexed { index, subVideo ->
                        println("$index")
                        val time = currentTime
                        // 실행시켜야되는지?
                        if ((subVideo.startingTime / 1000).toLong() <= time / 1000 &&
                            (subVideo.endingTime / 1000).toLong() >= time / 1000 &&
                            !isPlayings[index]
                        ) {
                            withContext(Dispatchers.Main) {
                                isPlayings[index] = true
                                binding.alphaView.setVideoFromUri(context, subVideo.uri.toUri())
                                binding.alphaView.mediaPlayer.setOnPreparedListener {
                                    binding.alphaView.mediaPlayer.start()
                                    binding.alphaView.setLooping(false)
                                    binding.alphaView.seekTo((time - subVideo.startingTime.toLong()).toInt())
                                    binding.alphaView.visibility = View.VISIBLE
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
                delay(500)
            }
        }
    }

    private fun setScanner() {
        viewModel.getSubVideo().size.let { repeat(it) { isPlayings.add(false) } }
    }

    private val onPlayStateChangeListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlay: Boolean) {
            isPlaying = isPlay
            if (isPlay) {
                jobScanner = lifecycleScope.launch { scan() }
                jobTimer = lifecycleScope.launch { timer() }
                jobScanner.start()
                jobTimer.start()
            } else {
                jobScanner.cancel()
                jobTimer.cancel()
                binding.alphaView.mediaPlayer.reset()
                isPlayings.forEachIndexed { index, _ -> isPlayings[index] = false }
                binding.alphaView.visibility = View.GONE
            }
        }
    }

    companion object {
        const val VIDEO_MODE_FRAME = 10000000
        const val VIDEO_MODE_SUB_VIDEO = 10000001
    }
}