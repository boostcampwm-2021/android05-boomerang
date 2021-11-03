package com.kotlinisgood.boomerang.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.kotlinisgood.boomerang.database.AppDatabase
import com.kotlinisgood.boomerang.database.dao.VideoMemoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "memo.db"
        ).build()
    }

    @Provides
    fun provideVideoMemoDao(database: AppDatabase): VideoMemoDao {
        return database.videoMemoDao()
    }

    @Provides
    fun provideContentResolver(@ApplicationContext appContext: Context): ContentResolver {
        return appContext.contentResolver
    }
}