package com.kotlinisgood.boomerang.util

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.jakewharton.rxbinding4.appcompat.navigationClicks
import com.jakewharton.rxbinding4.view.clicks
import java.util.concurrent.TimeUnit

const val DEFAULT_HEIGHT_WIDTH = -1
const val VIDEO_MODE_FRAME = 10000000
const val VIDEO_MODE_SUB_VIDEO = 10000001
const val AUDIO_MODE = 10000002
const val PREF_NAME = "SETTING"
const val ORDER_STATE = "orderState"


fun View.throttle(duration: Long, timeUnit: TimeUnit, method: () -> Unit) {
    this.clicks()
        .throttleFirst(duration, timeUnit)
        .subscribe {
            method()
        }
}

fun MenuItem.throttle(duration: Long, timeUnit: TimeUnit, method: () -> Unit) {
    this.clicks()
        .throttleFirst(duration, timeUnit)
        .subscribe {
            method()
        }
}

fun Toolbar.throttle(duration: Long, timeUnit: TimeUnit, method: () -> Unit) {
    this.navigationClicks()
        .throttleFirst(duration, timeUnit)
        .subscribe {
            method()
        }
}