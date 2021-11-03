package com.kotlinisgood.boomerang.ui.videoselection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kotlinisgood.boomerang.ui.home.ExternalVideoDTO
import com.kotlinisgood.boomerang.ui.home.VideoGallery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VideoSelectionViewModel @Inject constructor(
    private val videoGallery: VideoGallery
) : ViewModel() {

    private var _videoList = MutableLiveData<List<ExternalVideoDTO>>()
    val videoList: LiveData<List<ExternalVideoDTO>> = _videoList

    fun loadVideos() {
        _videoList.value = videoGallery.loadVideos()
    }
}