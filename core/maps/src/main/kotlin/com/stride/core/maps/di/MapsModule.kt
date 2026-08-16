package com.stride.core.maps.di

import android.content.Context
import com.stride.core.maps.PmtilesCache
import com.stride.core.maps.PmtilesConfig
import com.stride.core.maps.PmtilesRepository
import com.stride.core.maps.PmtilesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MapsModule {

    @Binds
    abstract fun bindPmtilesRepository(impl: PmtilesRepositoryImpl): PmtilesRepository

    companion object {
        @Provides
        @Singleton
        fun providePmtilesConfig(): PmtilesConfig = PmtilesConfig()

        @Provides
        @Singleton
        fun providePmtilesCache(@ApplicationContext context: Context): PmtilesCache =
            PmtilesCache(File(context.filesDir, "pmtiles"))
    }
}
