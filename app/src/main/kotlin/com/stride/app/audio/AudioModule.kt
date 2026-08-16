package com.stride.app.audio

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    // androidx.annotation.OptIn, not kotlin.OptIn — AndroidX Lint's UnsafeOptInUsageDetector
    // (what actually flags UnstableApi usage) only recognizes the former; kotlin.OptIn silences
    // the Kotlin *compiler*'s opt-in requirement but not this lint check.
    @OptIn(UnstableApi::class) // setPlaybackLooper — see the comment below on why it's used
    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
        ExoPlayer.Builder(context)
            // Explicit main-Looper binding, not "whichever thread first resolves this Hilt
            // singleton" — Hilt singletons are lazily built on first injection, and ExoPlayer's
            // playback thread otherwise binds to whatever thread calls build(). In practice
            // that's always main here (ViewModel creation, Service.onCreate are both
            // main-thread-guaranteed by the Android framework), but this removes the ambiguity
            // rather than relying on that happening to hold.
            .setPlaybackLooper(Looper.getMainLooper())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                // handleAudioFocus = false, deliberately — this is NOT the lazy default. Media3's
                // AudioFocusManager throws IllegalArgumentException for any usage other than
                // USAGE_MEDIA/USAGE_GAME when automatic handling is requested (confirmed by
                // actually crashing on-device: "Automatic handling of audio focus is only
                // available for USAGE_MEDIA and USAGE_GAME"). USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                // is exactly right for a cue, so the fix is hand-rolling the focus request/release
                // cycle instead (see Media3VoiceCueSpeaker) — not switching to the wrong usage
                // just to get the automatic path to work.
                false,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
}
