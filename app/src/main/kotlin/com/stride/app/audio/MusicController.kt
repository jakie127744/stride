package com.stride.app.audio

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play/pause/skip for whatever music app is already playing (Spotify, YouTube Music, a local
 * player) — dispatches standard media-button key events via [AudioManager], the same mechanism
 * a Bluetooth headset's physical buttons use. No notification-listener permission, no querying
 * what's installed: this works with anything that's already holding media-button focus, which
 * is exactly "the runner's own player, already open" per docs/foundation.md's third music tier
 * (duck-only coexistence). It cannot browse or pick a specific song — see docs/foundation.md
 * for that distinction (Spotify App Remote SDK vs. everything else).
 */
@Singleton
class MusicController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun playPause() = dispatch(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun next() = dispatch(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = dispatch(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    private fun dispatch(keyCode: Int) {
        val eventTime = android.os.SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
    }
}
