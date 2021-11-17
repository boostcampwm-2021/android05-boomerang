package com.kotlinisgood.boomerang.ui.audiomemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.kotlinisgood.boomerang.databinding.FragmentAudioMemoBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AudioMemoFragment : Fragment() {

    private var _dataBinding: FragmentAudioMemoBinding? = null
    val dataBinding get() = _dataBinding!!
    private val viewModel: AudioMemoViewModel by viewModels()
    private val args: AudioMemoFragmentArgs by navArgs()

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
        _dataBinding?.apply {
            viewModel = viewModel
            lifecycleOwner = viewLifecycleOwner
        }
        viewModel.getMediaMemo(args.mediaMemoId)
    }

    fun setObservers() {
        viewModel.mediaMemo.observe(viewLifecycleOwner) {
            // ToDo 가져온 값을 토대로 UI 세팅
        }
    }

    // ToDo recyclerView adapter 설정

    // PlayerControlView 설정

}