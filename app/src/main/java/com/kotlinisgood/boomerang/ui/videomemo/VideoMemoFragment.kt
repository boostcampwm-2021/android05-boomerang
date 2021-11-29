package com.kotlinisgood.boomerang.ui.videomemo

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.alphamovie.lib.AlphaMovieView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoMemoBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.ui.videosave.AlphaViewFactory
import com.kotlinisgood.boomerang.util.CustomLoadingDialog
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import com.kotlinisgood.boomerang.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VideoMemoFragment : Fragment() {

    private val loadingDialog by lazy { CustomLoadingDialog(requireContext()) }

    private var _dataBinding: FragmentVideoMemoBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val viewModelVideo: VideoMemoViewModel by viewModels()
    private val args: VideoMemoFragmentArgs by navArgs()

    private var currentTime = 0L
    private lateinit var player: ExoPlayer
    lateinit var jobScanner: Job

    private lateinit var alphaViewFactory: AlphaViewFactory
    private var currentAlpha: AlphaMovieView? = null
    private var currentSubVideo: SubVideo? = null

    private val compositeDisposable by lazy { CompositeDisposable() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding = FragmentVideoMemoBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        alphaViewFactory = AlphaViewFactory(requireContext())
        setViewModel()
        setPlayer()
        setMenuOnToolBar()
        setObserver()
    }

    private fun setObserver() {
        viewModelVideo.isLoading.observe(viewLifecycleOwner){ loading ->
            if(loading){
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }
    }

    private fun setMenuOnToolBar() {
        dataBinding.tbMemo.apply {
            inflateMenu(R.menu.menu_fragment_video_memo)
            if(args.memoType == VIDEO_MODE_SUB_VIDEO) menu.findItem(R.id.menu_video_memo_share).isVisible = false
            setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
            compositeDisposable.add(throttle(1000,TimeUnit.MILLISECONDS) {
                findNavController().popBackStack()
            })
            menu.forEach {
                when (it.itemId) {
                    R.id.menu_video_memo_share -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) {
                            val fileName = viewModelVideo.mediaMemo.value!!.mediaUri.split('/').last()
                            val file = File(requireContext().filesDir, fileName)
                            val uri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.kotlinisgood.boomerang.fileprovider",
                                file
                            )
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, uri)
                                type = "video/mp4"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)
                        })
                    }
                    R.id.menu_memo_modify -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) { checkModifyAndMove()})
                    }
                    R.id.menu_video_memo_delete -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) { showDeleteDialog() })
                    }
                }
            }
        }
    }

    private fun checkModifyAndMove() {
        println(viewModelVideo.mediaMemo.value)
        val action = viewModelVideo.mediaMemo.value?.id?.let { it ->
            VideoMemoFragmentDirections.actionMemoFragmentToVideoModifyLightFragment(
                it
            )
        }
        if (action != null) {
            findNavController().navigate(action)
        }
    }

    private fun showDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("메모 삭제")
            .setMessage("메모를 삭제하시겠습니까?")
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
        dataBinding.viewModel = viewModelVideo
        dataBinding.lifecycleOwner = viewLifecycleOwner
        viewModelVideo.loadMediaMemo(args.id)
    }

    fun setPlayer(){
        viewModelVideo.mediaMemo.observe(viewLifecycleOwner){ mediaMemo ->
            val mediaItem = MediaItem.fromUri(mediaMemo.mediaUri)
            player = ExoPlayer.Builder(requireContext()).build().apply {
                setMediaItem(mediaItem)
                addListener(onPlayStateChangeListener)
                prepare()
            }.also {
                dataBinding.exoplayer.player = it
                dataBinding.pcvVideoMemo.player = it
            }

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
                                    dataBinding.alphaView.removeAllViews()
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

                                    dataBinding.alphaView.addView(alphaMovieView)
                                    alphaMovieView.start()
                                    alphaMovieView.setOnVideoStartedListener {
                                        alphaMovieView.seekTo((currentTime - subVideo.startingTime.toLong()).toInt())
                                    }
                                    alphaMovieView.setOnVideoEndedListener {
                                        dataBinding.alphaView.removeAllViews()
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
                                dataBinding.alphaView.removeAllViews()
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
            pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.run {
            removeListener(onPlayStateChangeListener)
            stop()
            release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        _dataBinding = null
    }
}