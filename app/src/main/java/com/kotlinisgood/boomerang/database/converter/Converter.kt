package com.kotlinisgood.boomerang.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo

class Converter {
    @TypeConverter
    fun videosToJson(value: List<SubVideo>): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToVideos(value: String): List<SubVideo> = Gson().fromJson(value, Array<SubVideo>::class.java).toList()
}