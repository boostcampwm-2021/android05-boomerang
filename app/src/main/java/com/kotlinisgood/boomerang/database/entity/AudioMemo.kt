package com.kotlinisgood.boomerang.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_memo")
class AudioMemo (
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "audio_path") val path: String,
    @ColumnInfo(name = "create_date") val createTime: Long,
    @ColumnInfo(name = "text_list") val textList: List<String>,
    @ColumnInfo(name = "time_list") val timeList: List<Int>,
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}