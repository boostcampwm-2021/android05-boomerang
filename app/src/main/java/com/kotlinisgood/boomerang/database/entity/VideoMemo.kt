package com.kotlinisgood.boomerang.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo

@Entity(tableName = "video_memo")
data class VideoMemo(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "video_uri") val videoUri: String,
    @ColumnInfo(name = "memos") val memos: List<SubVideo>,
    @ColumnInfo(name = "create_date") val createTime: Long,
    @ColumnInfo(name = "edit_date") val editTime: Long,
    @ColumnInfo(name = "memo_type") val memoType: Int
){
    @PrimaryKey(autoGenerate = true) var id: Int=0
}
