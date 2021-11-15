package com.kotlinisgood.boomerang.ui.voicerecord

import androidx.lifecycle.ViewModel

class VoiceRecordViewModel : ViewModel() {
    val voiceList = mutableListOf<Voice>()

    fun addSubAudio(path: String, duration: Int, recognizedText: String) {
        voiceList.add(Voice(path, duration, recognizedText))
    }

}