package com.stride.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A minimal, real voice-cue engine ahead of the full Media3 audio engine (Phase 4). Deliberately
 * NOT the final architecture — see docs/foundation.md "Smart Audio Engine": that calls for a
 * foreground `MediaSessionService` + `ExoPlayer` so cues keep firing while the app is
 * backgrounded, with a proper request/release audio-focus cycle. This class answers "does it
 * talk when I switch from walk to run" honestly and correctly *while the app is in the
 * foreground*, using one real trick worth knowing: [AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE]
 * is the same usage type turn-by-turn nav apps use, and Android's audio framework ducks other
 * apps' media for it automatically — no manual `AudioFocusRequest` juggling needed for this
 * interim version. Once Phase 4 lands, this class goes away in favor of the real engine.
 */
@Singleton
class VoiceCueSpeaker @Inject constructor(
    @ApplicationContext context: Context,
) {
    @Volatile private var isReady = false

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        isReady = status == TextToSpeech.SUCCESS
    }.apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }

    fun speak(text: String) {
        if (!isReady) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "stride_cue")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
