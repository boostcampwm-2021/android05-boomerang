package com.kotlinisgood.boomerang.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo

@Entity(tableName = "media_memo")
data class MediaMemo(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "media_uri") val mediaUri: String,
    @ColumnInfo(name = "create_date") val createTime: Long,
    @ColumnInfo(name = "modify_date") var modifyTime: Long,
    @ColumnInfo(name = "memo_type") val memoType: Int,
    @ColumnInfo(name = "memo_list") var memoList: List<SubVideo>,
    @ColumnInfo(name = "text_list") val textList: List<String>,
    @ColumnInfo(name = "time_list") val timeList: List<Int>,
    @ColumnInfo(name = "memo_height") var memoHeight: Int,
    @ColumnInfo(name = "memo_width") var memoWidth: Int
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
