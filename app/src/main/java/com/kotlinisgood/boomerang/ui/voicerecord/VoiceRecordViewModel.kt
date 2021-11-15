package com.kotlinisgood.boomerang.ui.voicerecord

import androidx.lifecycle.ViewModel

class VoiceRecordViewModel : ViewModel() {
    val voiceList = listOf<Voice>()

    fun addSubAudio(path: String, duration: Int, recognizedText: String) {
        voiceList.plus(Voice(path, duration, recognizedText))
    }

}