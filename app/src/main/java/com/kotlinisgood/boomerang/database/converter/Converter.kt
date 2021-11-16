package com.kotlinisgood.boomerang.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo

class Converter {
    @TypeConverter
    fun videosToJson(value: List<SubVideo>): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToVideos(value: String): List<SubVideo> = Gson().fromJson(value, Array<SubVideo>::class.java).toList()

    @TypeConverter
    fun audioTextToJson(value: List<String>): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToAudioText(value: String): List<String> = Gson().fromJson(value, Array<String>::class.java).toList()

    @TypeConverter
    fun audioTimeToJson(value: List<Int>): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToAudioTime(value: String): List<Int> = Gson().fromJson(value, Array<Int>::class.java).toList()

}