package com.kotlinisgood.boomerang.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kotlinisgood.boomerang.database.entity.AudioMemo

@Dao
interface AudioMemoDao {
    @Query("SELECT * FROM audio_memo")
    suspend fun getAll(): List<AudioMemo>

    @Insert
    suspend fun insert(audio: AudioMemo)
}