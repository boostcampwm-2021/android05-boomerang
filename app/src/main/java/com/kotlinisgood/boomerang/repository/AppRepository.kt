package com.kotlinisgood.boomerang.repository

import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.local.LocalDataSource
import javax.inject.Inject

class AppRepository @Inject constructor(
    private val localDataSource: LocalDataSource
) {
    suspend fun getMediaMemos(): List<MediaMemo> {
        return localDataSource.getMediaMemos()
    }

    suspend fun getMediaMemo(id: Int): MediaMemo {
        return localDataSource.getMediaMemo(id)
    }

    suspend fun updateMediaMemo(memo: MediaMemo) {
        localDataSource.updateMediaMemo(memo)
    }

    suspend fun saveMediaMemo(memo: MediaMemo) {
        localDataSource.saveMediaMemo(memo)
    }

    suspend fun searchMediaByKeyword(query: String): List<MediaMemo> {
        return localDataSource.searchMedia(query)
    }

}