package com.kotlinisgood.boomerang.ui.videodoodlelight

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubVideo(var uri: Uri, val startingTime: Int, var endingTime: Int) : Parcelable
