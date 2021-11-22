package com.kotlinisgood.boomerang.ui.videomemo

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoMemoBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.ui.videoedit.AlphaViewFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class VideoMemoFragment : Fragment() {

    private lateinit var binding: FragmentVideoMemoBinding
    private val viewModelVideo: VideoMemoViewModel by viewModels()
    private val args: VideoMemoFragmentArgs by navArgs()

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
        binding = FragmentVideoMemoBinding.inflate(inflater, container, false)
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
        binding.tbMemo.apply {
            inflateMenu(R.menu.menu_fragment_video_memo)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_memo_modify -> {
                        println(viewModelVideo.mediaMemo.value)
                        val action = viewModelVideo.mediaMemo.value?.id?.let { it ->
                            VideoMemoFragmentDirections.actionMemoFragmentToVideoModifyLightFragment(
                                it
                            )
                        }
                        if (action != null) {
                            findNavController().navigate(action)
                        }
                        true
                    }
                    R.id.menu_video_memo_delete -> {
                        showDeleteDialog()
                        true
                    }
                    else -> false
                }
            }
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun showDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("메모 삭제")
            .setMessage("메몰를 삭제하시겠습니까?")
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("삭제") { dialog, _ ->
                lifecycleScope.launch {
                    val result = viewModelVideo.deleteMemo()
                    delay(500)
                    if (result) {
                        dialog.dismiss()
                        findNavController().navigate(VideoMemoFragmentDirections.actionAudioMemoFragmentToHomeFragment())
                    } else {
                        Toast.makeText(requireContext(), "메모 삭제에 실패하였습니다", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    fun setViewModel() {
        binding.viewModel = viewModelVideo
        binding.lifecycleOwner = viewLifecycleOwner
        viewModelVideo.loadMediaMemo(args.id)
    }

    fun setPlayer(){
        viewModelVideo.mediaMemo.observe(viewLifecycleOwner){ mediaMemo ->
            val mediaItem = MediaItem.fromUri(mediaMemo.mediaUri)
            player = SimpleExoPlayer.Builder(requireContext()).build().apply {
                setMediaItem(mediaItem)
                addListener(onPlayStateChangeListener)
                prepare()
            }
            binding.exoplayer.player = player

            mediaMemo.memoList.forEach {
                viewModelVideo.addAlphaMovieView(alphaViewFactory.create().apply {
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
                viewModelVideo.getSubVideoStates()
                    .forEachIndexed { index, _ -> viewModelVideo.getSubVideoStates()[index] = false }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun scan() {
        withContext(Dispatchers.Default) {
            while (true) {
                viewModelVideo.getSubVideo().forEachIndexed { index, subVideo ->
                    withContext(Dispatchers.Main) {
                        currentTime = player.currentPosition
                    }
                    if ((subVideo.startingTime).toLong() <= currentTime &&
                        (subVideo.endingTime).toLong() >= currentTime
                    ) {
                        if (!viewModelVideo.getSubVideoStates()[index]) {
                            withContext(Dispatchers.Main) {
                                // 현재 재생 중이던 영상과 subVideo 가 같을 때
                                if (subVideo == currentSubVideo) {
                                    viewModelVideo.getSubVideoStates()[index] = true
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

                                    val alphaMovieView = viewModelVideo.alphaMovieViews[index]
                                    viewModelVideo.getSubVideoStates()[index] = true
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
                    if(!viewModelVideo.getSubVideoStates().contains(true)) {
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
        val index = viewModelVideo.getSubVideo().indexOf(currentSubVideo)
        viewModelVideo.alphaMovieViews[index] = alphaView
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