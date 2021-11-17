package com.kotlinisgood.boomerang.ui.videodoodlelight

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*


class VideoDoodleLightViewModel : ViewModel() {

    private var duration = 0L

    private val _subVideos: MutableLiveData<List<SubVideo>> =
        MutableLiveData(listOf())
    val subVideos: LiveData<List<SubVideo>> get() = _subVideos

    private var currentSubVideo: SubVideo? = null

    private var recordStartTime = 0L
    var timer: Job? = null
    private val _timeOver: MutableLiveData<Boolean> = MutableLiveData(false)
    val timeOver: LiveData<Boolean> get() = _timeOver

    fun setCurrentSubVideo(subVideo: SubVideo) {
        currentSubVideo = subVideo
    }

    fun getCurrentSubVideo(): SubVideo? {
        return currentSubVideo
    }

    fun setDuration(time: Long){
        duration = time
    }

    fun startRecordTime(){
        recordStartTime = System.currentTimeMillis()
        timer = viewModelScope.launch{
            while(isActive) {
                val currentTime = System.currentTimeMillis()
                if(currentSubVideo!!.startingTime + currentTime - recordStartTime > duration){
                    println(currentSubVideo!!.startingTime + currentTime - recordStartTime)
                    println(duration)
                    _timeOver.value = true
                    break
                }
                delay(10)
            }
        }
    }

    fun resetTimer(){
        timer?.cancel()
        recordStartTime = 0L
    }

    fun setEndTime(duration: Int) {
        currentSubVideo!!.endingTime = currentSubVideo!!.startingTime + duration
        val subs = subVideos.value!!.filter {
                !((it.startingTime >= currentSubVideo!!.startingTime && currentSubVideo!!.endingTime >= it.startingTime))
        }.toMutableList()
        subs.add(currentSubVideo!!)
        currentSubVideo = null
        subs.sortBy{it.startingTime}
        _subVideos.value = subs
    }

    fun deleteSubVideo(position: Int) {
        val subs = subVideos.value!!.toMutableList()
        subs.removeAt(position)
        _subVideos.value = subs
    }
}