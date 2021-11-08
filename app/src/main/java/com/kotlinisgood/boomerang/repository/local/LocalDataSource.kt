package com.kotlinisgood.boomerang.repository.local

import com.kotlinisgood.boomerang.database.AppDatabase
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val db: AppDatabase,
) {
    suspend fun getVideoMemos(): List<VideoMemo> {
        return db.videoMemoDao().getAll()
    }

    suspend fun saveVideoMemo(memo: VideoMemo){
        db.videoMemoDao().insertAll(memo)
    }
}