package com.stride.core.database.di

import android.content.Context
import androidx.room.Room
import com.stride.core.database.StrideDatabase
import com.stride.core.database.dao.PlanDao
import com.stride.core.database.dao.RunSessionDao
import com.stride.core.database.dao.ShoeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStrideDatabase(@ApplicationContext context: Context): StrideDatabase =
        Room.databaseBuilder(context, StrideDatabase::class.java, StrideDatabase.DATABASE_NAME)
            // No destructive fallback: schema exports are versioned so real migrations can be
            // written once this ships — silently dropping a runner's history on upgrade isn't
            // acceptable for what's effectively their training log.
            .build()

    @Provides
    fun provideRunSessionDao(database: StrideDatabase): RunSessionDao = database.runSessionDao()

    @Provides
    fun providePlanDao(database: StrideDatabase): PlanDao = database.planDao()

    @Provides
    fun provideShoeDao(database: StrideDatabase): ShoeDao = database.shoeDao()
}
