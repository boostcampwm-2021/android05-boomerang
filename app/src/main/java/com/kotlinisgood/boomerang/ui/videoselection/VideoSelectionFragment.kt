package com.kotlinisgood.boomerang.ui.videoselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kotlinisgood.boomerang.databinding.FragmentVideoSelectionBinding
import com.kotlinisgood.boomerang.ui.home.VideoGallery

class VideoSelectionFragment : Fragment() {
    private var _dataBinding: FragmentVideoSelectionBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val videoSelectionAdapter by lazy {
        VideoSelectionAdapter(requireActivity().contentResolver)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _dataBinding = FragmentVideoSelectionBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.rvVideoSelectionShowVideos.adapter = videoSelectionAdapter
        setTbNavigationIconClickListener()
        loadVideos()
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
    }

    fun setTbNavigationIconClickListener() {
        dataBinding.tbVideoSelection.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    fun loadVideos() {
        videoSelectionAdapter.submitList(VideoGallery(requireActivity().contentResolver).loadVideos())
    }
}