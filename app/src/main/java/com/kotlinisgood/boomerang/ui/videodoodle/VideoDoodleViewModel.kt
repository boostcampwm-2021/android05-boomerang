package com.kotlinisgood.boomerang.ui.videodoodle

import androidx.lifecycle.ViewModel

class VideoDoodleViewModel : ViewModel() {

    var isEncoderWorking = false
    lateinit var encoder: Encoder

}