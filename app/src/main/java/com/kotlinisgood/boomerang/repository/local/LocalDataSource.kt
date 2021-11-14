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

    suspend fun updateVideoMemo(memo: VideoMemo){
        db.videoMemoDao().updateVideoMemo(memo)
    }

    suspend fun searchVideo(query: String): List<VideoMemo> {
        return db.videoMemoDao().search(query)
    }

    suspend fun getVideoMemo(id: Int): VideoMemo {
        return db.videoMemoDao().getVideoMemo(id)
    }
}