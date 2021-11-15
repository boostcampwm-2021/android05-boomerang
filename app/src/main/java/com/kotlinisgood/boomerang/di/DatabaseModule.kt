package com.kotlinisgood.boomerang.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kotlinisgood.boomerang.database.AppDatabase
import com.kotlinisgood.boomerang.database.dao.AudioMemoDao
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

        val migration_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE video_memo ADD COLUMN memo_type INTEGER")
                database.execSQL("UPDATE video_memo SET memo_type = '10000000' WHERE memos = '[]'")
                database.execSQL("UPDATE video_memo SET memo_type = '10000001' WHERE memos != '[]'")
            }
        }

        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "memo.db"
        )
            .addMigrations(migration_1_2)
            .build()
    }

    @Provides
    fun provideVideoMemoDao(database: AppDatabase): VideoMemoDao {
        return database.videoMemoDao()
    }

    @Provides
    fun provideAudioMemoDao(database: AppDatabase): AudioMemoDao {
        return database.audioMemoDao()
    }

}