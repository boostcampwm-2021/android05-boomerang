package com.kotlinisgood.boomerang.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kotlinisgood.boomerang.database.dao.VideoMemoDao
import com.kotlinisgood.boomerang.database.entity.VideoMemo

@Database(entities = [VideoMemo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoMemoDao(): VideoMemoDao
}