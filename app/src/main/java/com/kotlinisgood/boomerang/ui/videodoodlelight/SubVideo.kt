package com.kotlinisgood.boomerang.ui.videodoodlelight

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubVideo(var uri: String, val startingTime: Int, var endingTime: Int) : Parcelable
