package com.stride.core.health.di

import com.stride.core.health.HealthConnectRepository
import com.stride.core.health.HealthConnectRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {

    @Binds
    abstract fun bindHealthConnectRepository(impl: HealthConnectRepositoryImpl): HealthConnectRepository
}
