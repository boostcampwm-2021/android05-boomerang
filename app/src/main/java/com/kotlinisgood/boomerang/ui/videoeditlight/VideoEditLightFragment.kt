package com.kotlinisgood.boomerang.ui.videoeditlight

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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

    private lateinit var player: SimpleExoPlayer
    private lateinit var baseUri: Uri

    lateinit var job: Job

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
        setPlayer()
    }

    private fun setVideoView() {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        binding.exoplayer.player = player
        baseUri = args.baseVideo.toUri()
        val mediaItem = MediaItem.fromUri(baseUri)
        player.setMediaItem(mediaItem)
        var string = ""
        subVideos.forEach{
            string += "$it\n"
        }
        binding.tvSubvideos.text = string
    }

    private fun setPlayer() {
        player.addListener(onPlayStateChangeListener)
    }

    private val onPlayStateChangeListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            when (isPlaying) {
                true -> {
                    val currentTime = player.currentPosition
                    val videos =
                        subVideos.filter { it.startingTime > currentTime || (it.startingTime < currentTime && currentTime < it.endingTime) }
                    with(binding) {
                        job = CoroutineScope(Dispatchers.Main).launch {
                            videos.forEachIndexed { index, subVideo ->
                                if (index == 0) {
                                    delay(subVideo.startingTime.toLong() - currentTime)
                                } else {
                                    delay((subVideo.startingTime - subVideos[index - 1].startingTime).toLong())
                                }
                                alphaView.setVideoFromUri(context, subVideo.uri)
                                alphaView.mediaPlayer.setOnPreparedListener {
                                    alphaView.visibility = View.VISIBLE
                                    alphaView.setLooping(false)
                                    alphaView.setOnVideoEndedListener {
                                        alphaView.visibility = View.GONE
                                        alphaView.seekTo(0)
                                    }
                                    if (subVideo.startingTime < currentTime) {
                                        alphaView.seekTo((currentTime - subVideo.startingTime).toInt())
                                        alphaView.mediaPlayer.start()
                                    } else {
                                        alphaView.mediaPlayer.start()
                                    }
                                }
                            }
                        }
                    }
                }
                false -> {
                    job.cancel()
//                    binding.alphaView.mediaPlayer.pause()
                    binding.alphaView.visibility = View.GONE
                    binding.alphaView.seekTo(0)
                }
            }
        }
    }


}