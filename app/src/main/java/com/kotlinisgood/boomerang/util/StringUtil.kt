package com.kotlinisgood.boomerang.util

object StringUtil {

    fun convertMilliSec(duration: Int): String {
        val min = duration / 1000 / 60
        val secInt = duration / 1000 % 60
        val sec = if ( secInt < 10) {
            "0${secInt}"
        } else {
            (secInt).toString()
        }
        return "$min:$sec"
    }

}