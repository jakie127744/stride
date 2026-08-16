package com.stride.app.ui

/**
 * Interface boundary over the voice-cue engine — kept small and Android-framework-free so
 * `RunSessionEngine` (the highest-risk file in the app: state machine + concurrency + Room
 * writes) can be exercised in a plain JVM unit test against a fake, instead of needing a real
 * TTS/audio pipeline and Android context. See [com.stride.app.audio.Media3VoiceCueSpeaker] for
 * the real Phase 4 implementation and [com.stride.app.di.AppModule] for the Hilt binding.
 */
interface VoiceCueSpeaker {
    fun speak(text: String)
    fun shutdown()
}
