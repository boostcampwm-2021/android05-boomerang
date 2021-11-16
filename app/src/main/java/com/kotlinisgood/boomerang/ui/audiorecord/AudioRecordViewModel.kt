package com.kotlinisgood.boomerang.ui.audiorecord

import androidx.lifecycle.ViewModel
import com.kotlinisgood.boomerang.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AudioRecordViewModel @Inject constructor(repository: AppRepository): ViewModel() {

    val voiceList = mutableListOf<Audio>()

    fun addSubAudio(path: String, duration: Int, recognizedText: String) {
        voiceList.add(Audio(path, duration, recognizedText))
    }

    fun changeCurrentAudio(path: String, textList: List<String>, timeList: List<Int>) {
        println("Path : $path")
        println("TextList : $textList")
        println("TImeList : $timeList")
    }

}