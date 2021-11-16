package com.kotlinisgood.boomerang.database.dao

import androidx.room.*
import com.kotlinisgood.boomerang.database.entity.MediaMemo

@Dao
interface MediaMemoDao {
    @Query("SELECT * FROM media_memo")
    suspend fun getAll(): List<MediaMemo>

    @Query("SELECT * FROM media_memo WHERE id = :id")
    suspend fun getMediaMemo(id: Int): MediaMemo

    @Update
    suspend fun updateMediaMemo(vararg mediaMemos: MediaMemo)

    @Insert
    suspend fun insertAll(vararg mediaMemos: MediaMemo)

    @Delete
    suspend fun delete(mediaMemo: MediaMemo)

    @Query("SELECT * FROM media_memo WHERE title LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<MediaMemo>

}