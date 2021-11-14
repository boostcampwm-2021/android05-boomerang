package com.kotlinisgood.boomerang.di

import android.content.ContentResolver
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
object ContextModule {
    @Provides
    fun provideContentResolver(@ApplicationContext appContext: Context): ContentResolver {
        return appContext.contentResolver
    }
}