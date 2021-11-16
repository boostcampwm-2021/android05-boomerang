package com.kotlinisgood.boomerang.ui.videoedit

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.alphamovie.lib.AlphaMovieView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoEditBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File

@AndroidEntryPoint
class VideoEditFragment : Fragment() {

    private lateinit var binding: FragmentVideoEditBinding
    private val viewModel: VideoEditViewModel by viewModels()
    private val args: VideoEditFragmentArgs by navArgs()

    @Volatile
    private var currentTime = 0L
    private lateinit var player: SimpleExoPlayer
    lateinit var jobScanner: Job

    private lateinit var alphaViewFactory: AlphaViewFactory
    private var currentAlpha: AlphaMovieView? = null
    private var currentSubVideo: SubVideo? = null

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
        setAlphaViewFactory()
        setViewModel()
        setVideoView()
        setPlayer()
        setShareButton()
        setListener()
    }

    private fun setAlphaViewFactory() {
        alphaViewFactory = AlphaViewFactory(requireContext())
    }

    private fun setViewModel() {
        viewModel.setSubVideo(args.subVideos.toMutableList())
        viewModel.setVideoUri(args.baseVideo.toUri())
        viewModel.getSubVideo().forEach {
            val alphaView = alphaViewFactory.create()
            alphaView.setVideoFromUri(requireContext(), it.uri.toUri())
            viewModel.addAlphaMovieView(alphaView)
        }
    }

    private fun setShareButton() {
        if(args.subVideos.toList().isNotEmpty()) binding.btnShareMemo.visibility = View.GONE
    }

    private fun setListener() {
        binding.etMemoTitle.doOnTextChanged { text, start, before, count ->
            viewModel.setTitle(text.toString())
        }

        binding.btnSaveMemo.setOnClickListener {
            viewModel.saveMemo()
            findNavController().navigate(R.id.action_videoEditLightFragment_to_homeFragment)
        }

        binding.btnShareMemo.setOnClickListener {
            val fileName = args.baseVideo.split('/').last()
            println(fileName)
            val file = File(requireContext().filesDir, fileName)
            val uri = FileProvider.getUriForFile(requireContext(), "com.kotlinisgood.boomerang.fileprovider", file)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "video/mp4"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
    }

    private fun setVideoView() {
        val mediaItem = MediaItem.fromUri(viewModel.getVideoUri())
        player = SimpleExoPlayer.Builder(requireContext()).build()
        player.setMediaItem(mediaItem)
        binding.exoplayer.player = player
    }

    private fun setPlayer() {
        player.addListener(onPlayStateChangeListener)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun scan() {
        withContext(Dispatchers.Default) {
            while (true) {
                viewModel.getSubVideo().forEachIndexed { index, subVideo ->
                    withContext(Dispatchers.Main) {
                        currentTime = player.currentPosition
                    }
                    if ((subVideo.startingTime).toLong() <= currentTime &&
                        (subVideo.endingTime).toLong() >= currentTime
                    ) {
                        if (!viewModel.getSubVideoStates()[index]) {
                            withContext(Dispatchers.Main) {
                                // 현재 재생 중이던 영상과 subVideo 가 같을 때
                                if (subVideo == currentSubVideo) {
                                    viewModel.getSubVideoStates()[index] = true
                                    currentAlpha!!.mediaPlayer.seekTo(
                                        (player.currentPosition - subVideo.startingTime.toLong()),
                                        MediaPlayer.SEEK_CLOSEST
                                    )
                                    currentAlpha!!.mediaPlayer.start()
                                } else {
                                    binding.alphaView.removeAllViews()
                                    // 현재 재생 중이던 AlphaView 가 있을 시
                                    if (currentSubVideo != null) {
                                        resetAlphaView(currentSubVideo!!)
                                        currentSubVideo = null
                                        currentAlpha = null
                                    }

                                    val alphaMovieView = viewModel.alphaMovieViews[index]
                                    viewModel.getSubVideoStates()[index] = true
                                    currentSubVideo = subVideo
                                    currentAlpha = alphaMovieView

                                    binding.alphaView.addView(alphaMovieView)
                                    alphaMovieView.start()
                                    alphaMovieView.setOnVideoStartedListener {
                                        alphaMovieView.seekTo((currentTime - subVideo.startingTime.toLong()).toInt())
                                    }
                                    alphaMovieView.setOnVideoEndedListener {
                                        binding.alphaView.removeAllViews()
                                        resetAlphaView(currentSubVideo!!)
                                        currentSubVideo = null
                                        currentAlpha = null
                                    }
                                }
                            }
                        }
                    }
                    if(!viewModel.getSubVideoStates().contains(true)) {
                        withContext(Dispatchers.Main) {
                            if (currentSubVideo != null) {
                                binding.alphaView.removeAllViews()
                                resetAlphaView(currentSubVideo!!)
                                currentSubVideo = null
                                currentAlpha = null
                            }
                        }
                    }
                }
                delay(10)
            }
        }
    }

    private val onPlayStateChangeListener = object : Player.Listener {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onIsPlayingChanged(isPlay: Boolean) {
            if (isPlay) {
                jobScanner = lifecycleScope.launch { scan() }
            } else {
                jobScanner.cancel()
                if (currentAlpha != null && currentAlpha!!.isPlaying) {
                    currentAlpha!!.mediaPlayer.pause()
                }
                viewModel.getSubVideoStates()
                    .forEachIndexed { index, _ -> viewModel.getSubVideoStates()[index] = false }
            }
        }
    }

    private fun resetAlphaView(currentSubVideo: SubVideo) {
        val alphaView = alphaViewFactory.create()
        alphaView.setVideoFromUri(
            requireContext(),
            currentSubVideo.uri.toUri()
        )
        val index = viewModel.getSubVideo().indexOf(currentSubVideo)
        viewModel.alphaMovieViews[index] = alphaView
    }

    override fun onPause() {
        super.onPause()
        player.run {
            player.removeListener(onPlayStateChangeListener)
            stop()
            release()
        }
    }

    companion object {
        const val VIDEO_MODE_FRAME = 10000000
        const val VIDEO_MODE_SUB_VIDEO = 10000001
    }
}