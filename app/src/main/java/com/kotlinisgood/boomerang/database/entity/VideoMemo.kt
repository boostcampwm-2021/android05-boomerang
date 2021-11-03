package com.kotlinisgood.boomerang.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kotlinisgood.boomerang.model.Video

@Entity(tableName = "video_memo")
data class VideoMemo(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "video_uri") val videoUri: String,
    @ColumnInfo(name = "memos") val memos: List<Video>,
    @ColumnInfo(name = "create_date") val createTime: Long,
    @ColumnInfo(name = "edit_date") val editTime: Long,
    @ColumnInfo(name = "trash_date") val trashTime: Long,
)
