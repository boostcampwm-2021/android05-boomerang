package com.kotlinisgood.boomerang.repository

import com.kotlinisgood.boomerang.database.entity.VideoMemo
import com.kotlinisgood.boomerang.repository.local.LocalDataSource
import javax.inject.Inject

class AppRepository @Inject constructor(
    private val localDataSource: LocalDataSource
) {
    suspend fun getVideoMemos(): List<VideoMemo> {
        return localDataSource.getVideoMemos()
    }

    suspend fun saveVideoMemo(memo: VideoMemo){
        localDataSource.saveVideoMemo(memo)
    }

    suspend fun searchVideoByKeyword(query: String): List<VideoMemo> {
        return localDataSource.searchVideo(query)
    }
}