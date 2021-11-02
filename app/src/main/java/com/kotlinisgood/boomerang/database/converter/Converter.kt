package com.kotlinisgood.boomerang.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.kotlinisgood.boomerang.model.Video

class Converter {
    @TypeConverter
    fun videosToJson(value: List<Video>): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToVideos(value: String): List<Video> = Gson().fromJson(value, Array<Video>::class.java).toList()
}