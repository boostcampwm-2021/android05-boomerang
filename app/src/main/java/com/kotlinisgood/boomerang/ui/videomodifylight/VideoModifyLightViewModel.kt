package com.kotlinisgood.boomerang.ui.videomodifylight

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoModifyLightViewModel @Inject constructor(private val repository: AppRepository) : ViewModel(){

    private var _mediaMemo: MutableLiveData<MediaMemo> = MutableLiveData()
    val mediaMemo: LiveData<MediaMemo> get() = _mediaMemo

    private val _subVideos: MutableLiveData<List<SubVideo>> =
        MutableLiveData(listOf())
    val subVideos: LiveData<List<SubVideo>> get() = _subVideos

    private var currentSubVideo: SubVideo? = null

    fun setCurrentSubVideo(subVideo: SubVideo) {
        currentSubVideo = subVideo
    }

    fun loadVideoMemo(id: Int) {
        viewModelScope.launch {
            _mediaMemo.value = repository.getMediaMemo(id)
            _subVideos.value = mediaMemo.value?.memoList!!
        }
    }

    fun setEndTime(endTime: Int) {
        currentSubVideo!!.endingTime = endTime
        val subs = subVideos.value!!.filter {
            !((it.startingTime >= currentSubVideo!!.startingTime && currentSubVideo!!.endingTime >= it.startingTime))
        }.toMutableList()
        subs.add(currentSubVideo!!)
        currentSubVideo = null
        _subVideos.value = subs
    }

    fun updateVideoMemo() {
        val newVideoMemo = mediaMemo.value
        if (newVideoMemo != null) {
            newVideoMemo.memoList = subVideos.value!!
            newVideoMemo.modifyTime = System.currentTimeMillis()
            viewModelScope.launch {
                repository.updateMediaMemo(newVideoMemo)
            }
        }
    }

    fun deleteSubVideo(position: Int) {
        val subs = subVideos.value!!.toMutableList()
        subs.removeAt(position)
        _subVideos.value = subs
    }
}