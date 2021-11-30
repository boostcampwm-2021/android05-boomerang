package com.kotlinisgood.boomerang.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kotlinisgood.boomerang.database.AppDatabase
import com.kotlinisgood.boomerang.database.dao.MediaMemoDao
import com.kotlinisgood.boomerang.util.DEFAULT_HEIGHT_WIDTH
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

        val migrationFrom1to2 = object: Migration(1,2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_memo ADD COLUMN memo_height INTEGER DEFAULT $DEFAULT_HEIGHT_WIDTH NOT NULL")
                database.execSQL("ALTER TABLE media_memo ADD COLUMN memo_width INTEGER DEFAULT $DEFAULT_HEIGHT_WIDTH NOT NULL")
            }
        }

        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "media_memo.db"
        )
            .addMigrations(migrationFrom1to2)
            .build()
    }

    @Provides
    fun provideMediaMemoDao(database: AppDatabase): MediaMemoDao {
        return database.mediaMemoDao()
    }

}