package com.kotlinisgood.boomerang.model

import android.net.Uri

data class Video(
    val uri: Uri,
    val startSecond: Int,
    val endSecond: Int,
)