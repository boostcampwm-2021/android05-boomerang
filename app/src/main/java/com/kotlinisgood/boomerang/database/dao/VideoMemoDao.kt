package com.kotlinisgood.boomerang.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.kotlinisgood.boomerang.database.entity.VideoMemo

@Dao
interface VideoMemoDao {
    @Query("SELECT * FROM video_memo")
    fun getAll(): List<VideoMemo>

    @Insert
    fun insertAll(vararg videoMemos: VideoMemo)

    @Delete
    fun delete(videoMemo: VideoMemo)
}