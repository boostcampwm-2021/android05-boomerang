package com.kotlinisgood.boomerang.ui.memo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphamovie.lib.AlphaMovieView
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoViewModel @Inject constructor(private val repository: AppRepository) : ViewModel() {

    private var _mediaMemo: MutableLiveData<MediaMemo> = MutableLiveData()
    val mediaMemo: LiveData<MediaMemo> get() = _mediaMemo
    private var subVideos = listOf<SubVideo>()
    var alphaMovieViews: MutableList<AlphaMovieView> = mutableListOf<AlphaMovieView>()
    private var subVideosStates = mutableListOf<Boolean>()

    fun loadMediaMemo(id: Int) {
        viewModelScope.launch {
            _mediaMemo.value = repository.getMediaMemo(id)
            subVideos = mediaMemo.value?.memoList!!
            repeat(subVideos.size) { subVideosStates.add(false) }
        }
    }

    fun getSubVideo(): List<SubVideo> {
        return subVideos
    }

    fun getSubVideoStates(): MutableList<Boolean> {
        return subVideosStates
    }


    fun addAlphaMovieView(alphaMovieView: AlphaMovieView) {
        alphaMovieViews.add(alphaMovieView)
    }
}