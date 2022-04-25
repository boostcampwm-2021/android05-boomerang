package com.kotlinisgood.boomerang.ui.videomemo

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toFile
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
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.databinding.FragmentVideoMemoBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.ui.videosave.AlphaViewFactory
import com.kotlinisgood.boomerang.util.CustomLoadingDialog
import com.kotlinisgood.boomerang.util.Util.showSnackBar
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import com.kotlinisgood.boomerang.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VideoMemoFragment : Fragment() {

    private val loadingDialog by lazy { CustomLoadingDialog(requireContext()) }
    private var _dataBinding: FragmentVideoMemoBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val videoMemoViewModel: VideoMemoViewModel by viewModels()

    private val args: VideoMemoFragmentArgs by navArgs()
    private var currentTime = 0L
    private lateinit var player: ExoPlayer

    lateinit var jobScanner: Job
    private lateinit var alphaViewFactory: AlphaViewFactory
    private var currentAlpha: AlphaMovieView? = null

    private var currentSubVideo: SubVideo? = null

    private val compositeDisposable by lazy { CompositeDisposable() }

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
//                videoMemoViewModel.getSubVideoStates().forEachIndexed { idx, _ ->
//                    videoMemoViewModel.getSubVideoStates()[idx] = false
//                }
                currentSubVideo?.let {
                    videoMemoViewModel.getSubVideo().indexOf(it).also { idx ->
                        videoMemoViewModel.getSubVideoStates()[idx] = false
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding = FragmentVideoMemoBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBinding()
        setAlphaViewFactory()
        setMediaMemo()
        setMenuOnToolBar()
        setObserver()
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

    private fun setAlphaViewFactory() {
        alphaViewFactory = AlphaViewFactory(requireContext())
    }

    private fun setObserver() {
        videoMemoViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading) {
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }
        videoMemoViewModel.mediaMemo.observe(viewLifecycleOwner) { mediaMemo ->
            setVideoPlayer(mediaMemo)
            mediaMemo.memoList.forEach {
                videoMemoViewModel.addAlphaMovieView(alphaViewFactory.create().apply {
                    setVideoFromUri(requireContext(), it.uri.toUri())
                })
            }
        }
    }

    private fun setMenuOnToolBar() {
        dataBinding.tbVideoMemo.apply {
            inflateMenu(R.menu.menu_fragment_video_memo)
            if (args.memoType == VIDEO_MODE_SUB_VIDEO) {
                menu.findItem(R.id.menu_video_memo_share).isVisible = false
            }
            else {
                menu.findItem(R.id.menu_memo_modify).isVisible = false
            }
            setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
            compositeDisposable.add(throttle(1000, TimeUnit.MILLISECONDS) {
                findNavController().popBackStack()
            })
            menu.forEach {
                when (it.itemId) {
                    R.id.menu_video_memo_share -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) {
                            val file =
                                videoMemoViewModel.mediaMemo.value!!.mediaUri.toUri().toFile()
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
                        compositeDisposable.add(
                            it.throttle(
                                1000,
                                TimeUnit.MILLISECONDS
                            ) { checkModifyAndMove() })
                    }
                    R.id.menu_video_memo_delete -> {
                        compositeDisposable.add(
                            it.throttle(
                                1000,
                                TimeUnit.MILLISECONDS
                            ) { showDeleteDialog() })
                    }
                }
            }
        }
    }

    private fun setBinding() {
        dataBinding.apply {
            viewModel = videoMemoViewModel
            lifecycleOwner = viewLifecycleOwner
        }
    }

    private fun setVideoPlayer(mediaMemo: MediaMemo) {
        val mediaItem = MediaItem.fromUri(mediaMemo.mediaUri)
        player = ExoPlayer.Builder(requireContext()).build().apply {
            setMediaItem(mediaItem)
            addListener(onPlayStateChangeListener)
            prepare()
        }.also {
            dataBinding.exoplayerVideoMemo.player = it
            dataBinding.pcvVideoMemo.player = it
        }
    }

    private fun setMediaMemo() {
        videoMemoViewModel.loadMediaMemo(args.id)
    }

    private fun checkModifyAndMove() {
        videoMemoViewModel.mediaMemo.value?.id?.let { it ->
            VideoMemoFragmentDirections.actionMemoFragmentToVideoModifyLightFragment(it)
        }?.let {
            findNavController().navigate(it)
        }
    }

    private fun resetAlphaView(currentSubVideo: SubVideo) {
        alphaViewFactory.create().apply {
            setVideoFromUri(
                requireContext(),
                currentSubVideo.uri.toUri()
            )
        }.also { alphaMovieView ->
            videoMemoViewModel.getSubVideo().indexOf(currentSubVideo).also { idx ->
                videoMemoViewModel.alphaMovieViews[idx] = alphaMovieView
            }
        }

    }

    private fun showDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_memo_deletion_title))
            .setMessage(getString(R.string.dialog_memo_deletion_message))
            .setNegativeButton(getString(R.string.dialog_negative_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.dialog_positive_delete)) { dialog, _ ->
                lifecycleScope.launch {
                    videoMemoViewModel.deleteMemo().let { success ->
                        delay(500)
                        if (success) {
                            dialog.dismiss()
                            findNavController().navigate(
                                VideoMemoFragmentDirections.actionAudioMemoFragmentToHomeFragment()
                            )
                        } else {
                            dataBinding.containerVideoMemo.showSnackBar(
                                getString(R.string.dialog_memo_deletion_fail_message)
                            )
                            dialog.dismiss()
                        }
                    }
                }
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun scan() {
        withContext(Dispatchers.Default) {
            while (true) {
                var containsTrue = false
                videoMemoViewModel.getSubVideo().forEachIndexed { index, subVideo ->
                    withContext(Dispatchers.Main) {
                        currentTime = player.currentPosition
                    }
                    if ((subVideo.startingTime).toLong() <= currentTime &&
                        (subVideo.endingTime).toLong() >= currentTime
                    ) {
                        containsTrue = true
                        if (!videoMemoViewModel.getSubVideoStates()[index]) {
                            withContext(Dispatchers.Main) {
                                // 현재 재생 중이던 영상과 subVideo 가 같을 때
                                if (subVideo == currentSubVideo) {
                                    videoMemoViewModel.getSubVideoStates()[index] = true
                                    currentAlpha!!.mediaPlayer.seekTo(
                                        (player.currentPosition - subVideo.startingTime.toLong()),
                                        MediaPlayer.SEEK_CLOSEST
                                    )
                                    currentAlpha!!.mediaPlayer.start()
                                } else {
                                    dataBinding.alphaViewVideoMemo.removeAllViews()
                                    // 현재 재생 중이던 AlphaView 가 있을 시
                                    if (currentSubVideo != null) {
                                        resetAlphaView(currentSubVideo!!)
                                        currentSubVideo = null
                                        currentAlpha = null
                                    }

                                    val alphaMovieView = videoMemoViewModel.alphaMovieViews[index]
                                    videoMemoViewModel.getSubVideoStates()[index] = true
                                    currentSubVideo = subVideo
                                    currentAlpha = alphaMovieView

                                    dataBinding.alphaViewVideoMemo.addView(alphaMovieView)
                                    alphaMovieView.start()
                                    alphaMovieView.setOnVideoStartedListener {
                                        alphaMovieView.seekTo((currentTime - subVideo.startingTime.toLong()).toInt())
                                    }
                                    alphaMovieView.setOnVideoEndedListener {
                                        dataBinding.alphaViewVideoMemo.removeAllViews()
                                        resetAlphaView(currentSubVideo!!)
                                        currentSubVideo = null
                                        currentAlpha = null
                                    }
                                }
                            }
                        }
                    } else {
                        videoMemoViewModel.getSubVideoStates()[index] = false
                    }
                }
                if (!containsTrue) {
                    withContext(Dispatchers.Main) {
                        currentSubVideo?.let {
                            dataBinding.alphaViewVideoMemo.removeAllViews()
                            resetAlphaView(it)
                            currentSubVideo = null
                            currentAlpha = null
                        }
                    }
                }
                delay(10)
            }
        }
    }
}
