package com.kotlinisgood.boomerang.database.dao

import androidx.room.*
import com.kotlinisgood.boomerang.database.entity.VideoMemo

@Dao
interface VideoMemoDao {
    @Query("SELECT * FROM video_memo")
    suspend fun getAll(): List<VideoMemo>

    @Query("SELECT * FROM video_memo WHERE id = :id")
    suspend fun getVideoMemo(id: Int): VideoMemo

    @Update
    suspend fun updateVideoMemo(vararg videoMemos:VideoMemo)

    @Insert
    suspend fun insertAll(vararg videoMemos: VideoMemo)

    @Delete
    suspend fun delete(videoMemo: VideoMemo)

    @Query("SELECT * FROM video_memo WHERE title LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<VideoMemo>
}