package com.kotlinisgood.boomerang.repository.local

import com.kotlinisgood.boomerang.database.AppDatabase
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val db: AppDatabase,
) {
    suspend fun getMediaMemos(): List<MediaMemo> {
        return db.mediaMemoDao().getAll()
    }

    suspend fun saveMediaMemo(memo: MediaMemo) {
        db.mediaMemoDao().insertAll(memo)
    }

    suspend fun updateMediaMemo(memo: MediaMemo) {
        db.mediaMemoDao().updateMediaMemo(memo)
    }

    suspend fun searchMedia(query: String): List<MediaMemo> {
        return db.mediaMemoDao().search(query)
    }

    suspend fun getMediaMemo(id: Int): MediaMemo {
        return db.mediaMemoDao().getMediaMemo(id)
    }

    suspend fun deleteMemo(mediaMemo: MediaMemo) {
        db.mediaMemoDao().delete(mediaMemo)
    }

}