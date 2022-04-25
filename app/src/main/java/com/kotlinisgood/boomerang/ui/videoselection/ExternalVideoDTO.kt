package com.kotlinisgood.boomerang.ui.videoselection

import android.net.Uri

data class ExternalVideoDTO(
    val uri: Uri,
    val title: String,
    val duration: Int,
    val size: Int,
    var isChecked: Boolean = false
)
