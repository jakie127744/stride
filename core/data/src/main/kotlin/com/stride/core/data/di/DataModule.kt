package com.stride.core.data.di

import com.stride.core.data.repository.PlanRepository
import com.stride.core.data.repository.PlanRepositoryImpl
import com.stride.core.data.repository.RunRepository
import com.stride.core.data.repository.RunRepositoryImpl
import com.stride.core.data.repository.ShoeRepository
import com.stride.core.data.repository.ShoeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindRunRepository(impl: RunRepositoryImpl): RunRepository

    @Binds
    @Singleton
    abstract fun bindShoeRepository(impl: ShoeRepositoryImpl): ShoeRepository

    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository
}
