package com.stride.app.di

import com.stride.app.audio.Media3VoiceCueSpeaker
import com.stride.app.location.FusedLocationTracker
import com.stride.app.location.LocationTracker
import com.stride.app.ui.VoiceCueSpeaker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the app-local interfaces that exist specifically so `RunSessionEngine` is unit-testable
 * against fakes (see [VoiceCueSpeaker], [LocationTracker]) to their real Android implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindVoiceCueSpeaker(impl: Media3VoiceCueSpeaker): VoiceCueSpeaker

    @Binds
    abstract fun bindLocationTracker(impl: FusedLocationTracker): LocationTracker
}
