package com.kotlinisgood.boomerang.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.kotlinisgood.boomerang.database.entity.VideoMemo

@Dao
interface VideoMemoDao {
    @Query("SELECT * FROM video_memo")
    suspend fun getAll(): List<VideoMemo>

    @Insert
    suspend fun insertAll(vararg videoMemos: VideoMemo)

    @Delete
    suspend fun delete(videoMemo: VideoMemo)
}