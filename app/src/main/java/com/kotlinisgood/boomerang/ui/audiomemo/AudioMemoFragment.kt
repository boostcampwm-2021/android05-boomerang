package com.kotlinisgood.boomerang.ui.audiomemo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SeekParameters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentAudioMemoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class AudioMemoFragment : Fragment() {

    private val TAG = "AudioMemoFragment"
    private var _dataBinding: FragmentAudioMemoBinding? = null
    val dataBinding get() = _dataBinding!!
    private val viewModel: AudioMemoViewModel by viewModels()
    private val args: AudioMemoFragmentArgs by navArgs()
    private val audioMemoAdapter = AudioMemoAdapter()
    private lateinit var player: ExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding = FragmentAudioMemoBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBinding()
        setObservers()
        setAdapters()
        setMenusOnToolbar()
        viewModel.getMediaMemo(args.mediaMemoId)
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.mediaMemo.value != null) {
            player.prepare()
        }
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onStop() {
        super.onStop()
        player.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
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
        player = ExoPlayer.Builder(requireContext()).build()
            .apply {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
                setSeekParameters(SeekParameters.CLOSEST_SYNC)
                addListener(object: Player.Listener {
                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                        viewModel.modifyFocusedTextOrNot(newPosition.positionMs, audioMemoAdapter.currentList.toList())
                    }
                })
                prepare()
            }.also {
                dataBinding.pcvAudioMemoControlAudio.player = it
            }
        dataBinding.pcvAudioMemoControlAudio.setProgressUpdateListener { position, _ ->
            viewModel.modifyFocusedTextOrNot(position, audioMemoAdapter.currentList.toList())
        }
    }

    private fun setAdapters() {
        audioMemoAdapter.apply {
            setOnAudioMemoItemClickListener(object : AudioMemoAdapter.OnAudioMemoItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    player.seekTo((currentList[position].time.toLong()))
                }
            })
        }
        dataBinding.rvAudioMemoRecognizedText.apply {
            adapter = audioMemoAdapter
            itemAnimator = null
        }
    }

    private fun setMenusOnToolbar() {
        dataBinding.tbAudioMemo.apply {
            inflateMenu(R.menu.menu_fragment_audio_memo)
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_audio_memo_delete -> {
                        showDeleteDialog()
                        true
                    }
                    else -> false
                }
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
                    val result = viewModel.deleteMemo()
                    if (result) {
                        dialog.dismiss()
                        findNavController().navigate(AudioMemoFragmentDirections.actionAudioMemoFragmentToHomeFragment())
                    } else {
                        Toast.makeText(requireContext(), "메모 삭제에 실패하였습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

}