package com.kotlinisgood.boomerang.ui.voicerecord

import androidx.lifecycle.ViewModel

class VoiceRecordViewModel : ViewModel() {
    val list = listOf<Voice>()
    fun addSubAudio(path: String, duration: Int) {
        list.plus(Voice(path, duration))
    }

}