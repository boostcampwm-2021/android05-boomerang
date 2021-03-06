package com.kotlinisgood.boomerang.ui.audiomemo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
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
import com.jakewharton.rxbinding4.view.clicks
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentAudioMemoBinding
import com.kotlinisgood.boomerang.util.CustomLoadingDialog
import com.kotlinisgood.boomerang.util.Util.showSnackBar
import com.kotlinisgood.boomerang.util.throttle
import com.kotlinisgood.boomerang.util.throttle1000
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AudioMemoFragment : Fragment() {

    private var _dataBinding: FragmentAudioMemoBinding? = null
    val dataBinding get() = _dataBinding!!
    private val fragmentContainer by lazy { dataBinding.containerFragmentAudioMemo }

    private val audioMemoViewModel: AudioMemoViewModel by viewModels()
    private val args: AudioMemoFragmentArgs by navArgs()
    private val audioMemoAdapter = AudioMemoAdapter()
    private lateinit var player: ExoPlayer
    private val compositeDisposable by lazy { CompositeDisposable() }

    private val loadingDialog by lazy { CustomLoadingDialog(requireContext()) }

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
        audioMemoViewModel.getMediaMemo(args.mediaMemoId)
    }


    override fun onStart() {
        super.onStart()
        val mediaMemo = audioMemoViewModel.mediaMemo.value
        if (this::player.isInitialized) {
            player.prepare()
        }
        if (mediaMemo != null && !this::player.isInitialized) {
            setAudioPlayer(mediaMemo.mediaUri)
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
        compositeDisposable.dispose()
    }

    private fun setBinding() {
        dataBinding.apply {
            viewModel = audioMemoViewModel
            lifecycleOwner = viewLifecycleOwner
        }
    }

    private fun setObservers() {
        audioMemoViewModel.mediaMemo.observe(viewLifecycleOwner) {
            setAudioPlayer(it.mediaUri)
        }
        audioMemoViewModel.isLoading.observe(viewLifecycleOwner){ loading ->
            if(loading){
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }
    }

    private fun setAudioPlayer(path: String) {
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
                        audioMemoViewModel.modifyFocusedTextOrNot(newPosition.positionMs, audioMemoAdapter.currentList.toList())
                    }
                })
                prepare()
            }.also {
                dataBinding.pcvAudioMemoControlAudio.player = it
            }
        dataBinding.pcvAudioMemoControlAudio.setProgressUpdateListener { position, _ ->
            audioMemoViewModel.modifyFocusedTextOrNot(position, audioMemoAdapter.currentList.toList())
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
            compositeDisposable.add(throttle(throttle1000, TimeUnit.MILLISECONDS) {
                findNavController().popBackStack()
            })
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_audio_memo_delete -> {
                        compositeDisposable.add(it.clicks()
                            .throttleFirst(throttle1000, TimeUnit.MILLISECONDS)
                            .subscribe {
                                showDeleteDialog()
                            })
                        true
                    }
                    else -> false
                }
            }
            menu.forEach {
                when (it.itemId) {
                    R.id.menu_audio_memo_delete -> {
                        compositeDisposable.add(it.throttle(throttle1000, TimeUnit.MILLISECONDS) { showDeleteDialog() })
                    }
                }
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
                    val result = audioMemoViewModel.deleteMemo()
                    if (result) {
                        dialog.dismiss()
                        findNavController().navigate(AudioMemoFragmentDirections.actionAudioMemoFragmentToHomeFragment())
                    } else {
                        fragmentContainer.showSnackBar(getString(R.string.fragment_audio_memo_dialog_deletion_positive))
                    }
                }
            }
            .show()
    }

}