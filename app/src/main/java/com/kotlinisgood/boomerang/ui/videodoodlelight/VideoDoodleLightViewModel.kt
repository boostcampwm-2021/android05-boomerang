package com.kotlinisgood.boomerang.ui.videodoodlelight

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class VideoDoodleLightViewModel : ViewModel() {

    private val _subVideos: MutableLiveData<List<SubVideo>> =
        MutableLiveData(listOf())
    val subVideos: LiveData<List<SubVideo>> get() = _subVideos

    private var currentSubVideo: SubVideo? = null

    fun setCurrentSubVideo(subVideo: SubVideo) {
        currentSubVideo = subVideo
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
}