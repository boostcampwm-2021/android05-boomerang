package com.kotlinisgood.boomerang.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kotlinisgood.boomerang.database.converter.Converter
import com.kotlinisgood.boomerang.database.dao.MediaMemoDao
import com.kotlinisgood.boomerang.database.entity.MediaMemo

@Database(entities = [MediaMemo::class], version = 2, exportSchema = false)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaMemoDao(): MediaMemoDao
}