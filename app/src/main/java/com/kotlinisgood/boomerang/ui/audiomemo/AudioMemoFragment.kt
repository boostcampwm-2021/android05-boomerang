package com.kotlinisgood.boomerang.ui.audiomemo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.kotlinisgood.boomerang.databinding.FragmentAudioMemoBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AudioMemoFragment : Fragment() {

    private val TAG = "AudioMemoFragment"
    private var _dataBinding: FragmentAudioMemoBinding? = null
    val dataBinding get() = _dataBinding!!
    private val viewModel: AudioMemoViewModel by viewModels()
    private val args: AudioMemoFragmentArgs by navArgs()
    private val audioMemoAdapter = AudioMemoAdapter()
    private lateinit var player: SimpleExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _dataBinding = FragmentAudioMemoBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBinding()
        setObservers()
        setAdapters()
        viewModel.getMediaMemo(args.mediaMemoId)
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
    }

    private fun setBinding() {
        dataBinding.viewModel = viewModel
        dataBinding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setObservers() {
        viewModel.mediaMemo.observe(viewLifecycleOwner) {
            setPlayer(it.mediaUri)
        }
    }

    private fun setPlayer(path: String) {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        player.also {
            it.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            dataBinding.pcvAudioMemoControlAudio.player = it
        }
    }

    private fun setAdapters() {
        audioMemoAdapter.apply {
            setOnAudioMemoItemClickListener(object: AudioMemoAdapter.OnAudioMemoItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val item = currentList[position]
                    Log.i(TAG, "${item.time}")
                    player.seekTo((item.time.toLong()))
                }
            })
        }
        dataBinding.rvAudioMemoRecognizedText.adapter = audioMemoAdapter
    }
    // PlayerControlView 설정

}