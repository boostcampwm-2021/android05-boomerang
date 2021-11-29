package com.kotlinisgood.boomerang.ui.videosave

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.alphamovie.lib.AlphaMovieView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoSaveBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.util.CustomLoadingDialog
import com.kotlinisgood.boomerang.util.UriUtil
import com.kotlinisgood.boomerang.util.Util.showSnackBar
import com.kotlinisgood.boomerang.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VideoSaveFragment : Fragment() {

    private var _dataBinding: FragmentVideoSaveBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val videoSaveViewModel: VideoSaveViewModel by viewModels()
    private val args: VideoSaveFragmentArgs by navArgs()

    @Volatile
    private var currentTime = 0L

    private lateinit var player: ExoPlayer
    private lateinit var jobScanner: Job

    private var currentAlpha: AlphaMovieView? = null
    private var currentSubVideo: SubVideo? = null
    private val alphaViewFactory by lazy { AlphaViewFactory(requireContext()) }

    private val compositeDisposable by lazy { CompositeDisposable() }

    private val loadingDialog by lazy { CustomLoadingDialog(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding = FragmentVideoSaveBinding.inflate(
            inflater,
            container,
            false
        )
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewModel()
        setListener()
        setVideoView()
    }

    private fun setViewModel() {
        videoSaveViewModel.setSubVideo(args.subVideos.toMutableList())
        videoSaveViewModel.setVideoUri(args.baseVideo.toUri())
        videoSaveViewModel.setMemoType(args.memoType)
        videoSaveViewModel.getSubVideo().forEach {
            val alphaView = alphaViewFactory.create()
            alphaView.setVideoFromUri(requireContext(), it.uri.toUri())
            videoSaveViewModel.addAlphaMovieView(alphaView)
        }
    }

    private fun setListener() {
        compositeDisposable.add(dataBinding.tbVideoDoodle.throttle(1000, TimeUnit.MILLISECONDS) {
            findNavController().popBackStack()
        })

        dataBinding.tbVideoDoodle.inflateMenu(R.menu.menu_fragment_video_edit)

        dataBinding.tbVideoDoodle.menu.findItem(R.id.menu_video_edit_share).isVisible =
            args.memoType
        dataBinding.tbVideoDoodle.menu.forEach {
            when (it.itemId) {
                R.id.menu_video_edit_share -> {
                    compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) {
                        val fileName = args.baseVideo.split('/').last()
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
                R.id.menu_video_edit_save -> {
                    compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) {
                        if (args.memoType) {
                            if (videoSaveViewModel.getTitle().isEmpty()) {
                                dataBinding.containerVideoSave.showSnackBar(getString(R.string.snackbar_video_save_enter_title))
                            } else {
                                val heightWidth =
                                    getMediaSize(videoSaveViewModel.getVideoUri().path!!)
                                videoSaveViewModel.saveMemo(heightWidth.first, heightWidth.second)
                                findNavController().navigate(R.id.action_videoEditFragment_to_homeFragment)
                            }
                        } else {
                            if (videoSaveViewModel.getTitle().isEmpty()) {
                                dataBinding.containerVideoSave.showSnackBar(getString(R.string.snackbar_video_save_enter_title))
                            } else {
                                loadingDialog.show()
                                val file = File(
                                    requireContext().filesDir,
                                    System.currentTimeMillis().toString()
                                )
                                if (Build.VERSION.SDK_INT == 29) {
                                    try {
                                        val input =
                                            requireActivity().contentResolver.openInputStream(args.baseVideo.toUri())
                                        val output = FileOutputStream(file)

                                        val bytes = ByteArray(1024)
                                        var read = input?.read(bytes)!!
                                        while (read != -1) {
                                            output.write(bytes, 0, read)
                                            read = input.read(bytes)
                                        }
                                        input.close()
                                        output.close()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    val videoFile = File(
                                        UriUtil.getPathFromUri(
                                            requireContext().contentResolver,
                                            args.baseVideo.toUri()
                                        )
                                    )
                                    videoFile.copyTo(file)
                                }
                                val heightWidth = getMediaSize(file.path)
                                videoSaveViewModel.saveMemo(
                                    file.toUri().toString(),
                                    heightWidth.first,
                                    heightWidth.second
                                )
                                loadingDialog.dismiss()
                                findNavController().navigate(R.id.action_videoEditFragment_to_homeFragment)
                            }
                        }
                    })
                }
            }
        }
        dataBinding.etVideoMemoTitle.doOnTextChanged { text, _, _, _ ->
            videoSaveViewModel.setTitle(text.toString())
        }
    }

    private fun setVideoView() {
        player = ExoPlayer.Builder(requireContext()).build().apply {
            setMediaItem(MediaItem.fromUri(videoSaveViewModel.getVideoUri()))
            addListener(onPlayStateChangeListener)
            prepare()
        }.also {
            dataBinding.pcvVideoEdit.player = it
            dataBinding.exoplayer.player = it
        }
    }

    private fun getMediaSize(path: String): Pair<Int, Int> {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(path)
        val height =
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toInt()!!
        val width =
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toInt()!!
        return Pair(height, width)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun scan() {
        withContext(Dispatchers.Default) {
            while (true) {
                videoSaveViewModel.getSubVideo().forEachIndexed { index, subVideo ->
                    withContext(Dispatchers.Main) {
                        currentTime = player.currentPosition
                    }
                    // 현재 재생 위치에서 subVideo 가 재생 되어야할 때
                    if ((subVideo.startingTime).toLong() <= currentTime &&
                        currentTime <= (subVideo.endingTime).toLong()
                    ) {
                        // 해당 subVideo 가 재생중이지 않을 때
                        if (!videoSaveViewModel.getSubVideoStates()[index]) {
                            withContext(Dispatchers.Main) {
                                // 현재 subVideo 와 해당 subVideo 가 같을 때
                                if (subVideo == currentSubVideo) {
                                    videoSaveViewModel.getSubVideoStates()[index] = true
                                    currentAlpha!!.mediaPlayer.seekTo(
                                        (player.currentPosition - subVideo.startingTime.toLong()),
                                        MediaPlayer.SEEK_CLOSEST
                                    )
                                    currentAlpha!!.mediaPlayer.start()
                                } else {
                                    dataBinding.containerAlphaView.removeAllViews()
                                    // 현재 재생 중이던 AlphaView 가 있을 시
                                    if (currentSubVideo != null) {
                                        resetAlphaView(currentSubVideo!!)
                                        currentSubVideo = null
                                        currentAlpha = null
                                    }

                                    val alphaMovieView = videoSaveViewModel.alphaMovieViews[index]

                                    videoSaveViewModel.getSubVideoStates()[index] = true
                                    currentSubVideo = subVideo
                                    currentAlpha = alphaMovieView

                                    dataBinding.containerAlphaView.addView(alphaMovieView)
                                    alphaMovieView.start()
                                    alphaMovieView.setOnVideoStartedListener {
                                        alphaMovieView.seekTo((currentTime - subVideo.startingTime.toLong()).toInt())
                                    }

                                    alphaMovieView.setOnVideoEndedListener {
                                        dataBinding.containerAlphaView.removeAllViews()
                                        resetAlphaView(currentSubVideo!!)
                                        currentSubVideo = null
                                        currentAlpha = null
                                    }
                                }
                            }
                        }
                    } else {
                        // 현재 재생중인 subVideo 가 없는데 재생 표시가 있는 경우
                        if (!videoSaveViewModel.getSubVideoStates().contains(true)) {
                            withContext(Dispatchers.Main) {
                                if (currentSubVideo != null) {
                                    dataBinding.containerAlphaView.removeAllViews()
                                    resetAlphaView(currentSubVideo!!)
                                    currentSubVideo = null
                                    currentAlpha = null
                                }
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
                videoSaveViewModel.getSubVideoStates()
                    .forEachIndexed { index, _ ->
                        videoSaveViewModel.getSubVideoStates()[index] = false
                    }
            }
        }
    }

    private fun resetAlphaView(currentSubVideo: SubVideo) {
        val alphaView = alphaViewFactory.create()
        alphaView.setVideoFromUri(
            requireContext(),
            currentSubVideo.uri.toUri()
        )
        val index = videoSaveViewModel.getSubVideo().indexOf(currentSubVideo)
        videoSaveViewModel.getSubVideoStates()[index] = false
        videoSaveViewModel.alphaMovieViews[index] = alphaView
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
            stop()
            player.removeListener(onPlayStateChangeListener)
            release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        _dataBinding = null
    }
}