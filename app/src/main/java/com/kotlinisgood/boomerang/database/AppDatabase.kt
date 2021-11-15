package com.kotlinisgood.boomerang.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kotlinisgood.boomerang.database.converter.Converter
import com.kotlinisgood.boomerang.database.dao.AudioMemoDao
import com.kotlinisgood.boomerang.database.dao.VideoMemoDao
import com.kotlinisgood.boomerang.database.entity.AudioMemo
import com.kotlinisgood.boomerang.database.entity.VideoMemo

@Database(entities = [VideoMemo::class], version = 2, exportSchema = false)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoMemoDao(): VideoMemoDao
    abstract fun audioMemoDao(): AudioMemoDao
}