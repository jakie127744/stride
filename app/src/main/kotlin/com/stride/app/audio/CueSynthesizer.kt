package com.stride.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Synthesizes a cue's text to a short WAV file rather than speaking it directly — that file then
 * plays through [RunAudioService]'s `ExoPlayer`, which is what actually puts Stride on Media3's
 * real audio-focus/ducking pipeline (see docs/foundation.md "Smart Audio Engine") instead of
 * relying on `TextToSpeech`'s own implicit ducking via `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` —
 * the honest-but-fragile trick the interim engine used before this landed.
 *
 * One [UtteranceProgressListener] for the engine's whole lifetime, dispatching by utterance id
 * through [pending] — not a fresh listener per call, which would race if two cues were ever
 * synthesized concurrently (the second registration would silently orphan the first call's
 * continuation).
 */
@Singleton
class CueSynthesizer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile private var isReady = false
    private val pending = ConcurrentHashMap<String, CancellableContinuation<File?>>()

    private val cacheDir: File
        get() = File(context.cacheDir, "cue_audio").apply { mkdirs() }

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        isReady = status == TextToSpeech.SUCCESS
    }.apply {
        setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = complete(utteranceId, success = true)

                @Deprecated("Deprecated in Java, still required to override")
                override fun onError(utteranceId: String?) = complete(utteranceId, success = false)
                override fun onError(utteranceId: String?, errorCode: Int) = complete(utteranceId, success = false)
            },
        )
    }

    private fun complete(utteranceId: String?, success: Boolean) {
        val continuation = utteranceId?.let { pending.remove(it) } ?: return
        if (!continuation.isActive) return
        continuation.resume(if (success) File(cacheDir, "$utteranceId.wav") else null)
    }

    /** Null if TTS isn't ready or synthesis failed — a broken cue must never crash or block a
     * run in progress, same principle used everywhere else network/hardware can fail in this
     * app (see WeatherRepository, PmtilesRepository). */
    suspend fun synthesizeToFile(text: String): File? {
        if (!isReady || text.isBlank()) return null
        val utteranceId = UUID.randomUUID().toString()
        val file = File(cacheDir, "$utteranceId.wav")
        return suspendCancellableCoroutine { continuation ->
            pending[utteranceId] = continuation
            continuation.invokeOnCancellation { pending.remove(utteranceId) }
            val result = tts.synthesizeToFile(text, null, file, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                pending.remove(utteranceId)
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    fun shutdown() {
        runCatching { cacheDir.listFiles()?.forEach { it.delete() } }
        tts.stop()
        tts.shutdown()
    }
}
