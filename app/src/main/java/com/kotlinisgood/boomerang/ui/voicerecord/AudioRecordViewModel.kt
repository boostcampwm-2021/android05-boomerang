package com.kotlinisgood.boomerang.ui.voicerecord

import androidx.lifecycle.ViewModel

class AudioRecordViewModel : ViewModel() {
    val voiceList = mutableListOf<Audio>()

    fun addSubAudio(path: String, duration: Int, recognizedText: String) {
        voiceList.add(Audio(path, duration, recognizedText))
    }

}