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
}