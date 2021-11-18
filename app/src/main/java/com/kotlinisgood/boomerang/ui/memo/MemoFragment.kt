package com.kotlinisgood.boomerang.ui.memo

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
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
import com.kotlinisgood.boomerang.databinding.FragmentMemoBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.ui.videoedit.AlphaViewFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class MemoFragment : Fragment() {

    private lateinit var binding: FragmentMemoBinding
    private val viewModel: MemoViewModel by viewModels()
    private val args: MemoFragmentArgs by navArgs()

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
        binding = FragmentMemoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        alphaViewFactory = AlphaViewFactory(requireContext())
        setViewModel()
        setPlayer()
        setMenuOnToolBar()
    }

    private fun setMenuOnToolBar() {
        binding.tbMemo.inflateMenu(R.menu.menu_fragment_memo)
        binding.tbMemo.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_memo_modify -> {
                    println(viewModel.mediaMemo.value)
                    val action = viewModel.mediaMemo.value?.id?.let { it ->
                        MemoFragmentDirections.actionMemoFragmentToVideoModifyLightFragment(
                            it
                        )
                    }
                    if (action != null) {
                        findNavController().navigate(action)
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun setViewModel() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.loadMediaMemo(args.id)
    }

    fun setPlayer(){
        viewModel.mediaMemo.observe(viewLifecycleOwner){ mediaMemo ->
            val mediaItem = MediaItem.fromUri(mediaMemo.mediaUri)
            player = SimpleExoPlayer.Builder(requireContext()).build().apply {
                setMediaItem(mediaItem)
                addListener(onPlayStateChangeListener)
                prepare()
            }
            binding.exoplayer.player = player

            mediaMemo.memoList.forEach {
                viewModel.addAlphaMovieView(alphaViewFactory.create().apply {
                    setVideoFromUri(requireContext(), it.uri.toUri())
                })
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
            removeListener(onPlayStateChangeListener)
            stop()
            release()
        }
    }
}