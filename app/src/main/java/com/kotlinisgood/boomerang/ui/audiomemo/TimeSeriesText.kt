package com.kotlinisgood.boomerang.ui.audiomemo

data class TimeSeriesText(
    val id: Int,
    val time: Int,
    val text: String,
    val focused: Boolean = false
)
