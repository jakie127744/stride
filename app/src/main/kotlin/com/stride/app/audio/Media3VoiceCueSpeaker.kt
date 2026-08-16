package com.stride.app.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.stride.app.ui.VoiceCueSpeaker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real Phase 4 voice-cue engine: synthesizes each cue to a short file ([CueSynthesizer]) and
 * plays it through the shared [ExoPlayer] (see [AudioModule]).
 *
 * Ducking here is a hand-rolled [AudioFocusRequest] request/release cycle around each cue, NOT
 * ExoPlayer's automatic focus handling — that was the first thing tried, and it crashes on any
 * real device: Media3's `AudioFocusManager` only supports automatic handling for
 * `USAGE_MEDIA`/`USAGE_GAME`, and throws for `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` (confirmed by
 * actually running this on an emulator, not just compiling it — see git history). Requesting
 * `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` explicitly is what docs/foundation.md's "request transient
 * duck focus, play a clip, release focus" spec literally describes anyway.
 *
 * Replaces the interim `AndroidVoiceCueSpeaker`, whose doc comment always said it would go away
 * once this landed.
 *
 * Honest limitation: playback only actually reaches the runner's ears while [RunAudioService] is
 * running (started alongside `RunSessionService` for the run's duration — see
 * `ActiveRunViewModel`).
 */
@Singleton
class Media3VoiceCueSpeaker @Inject constructor(
    @ApplicationContext context: Context,
    private val synthesizer: CueSynthesizer,
    private val player: ExoPlayer,
) : VoiceCueSpeaker {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Main.immediate — ExoPlayer's calls must happen on the Looper it was built with
    // (see AudioModule: explicitly the main Looper), not whatever thread happens to call speak().
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile private var focusRequest: AudioFocusRequest? = null

    init {
        // Focus is released once the clip finishes playing, not on a fixed timer — a cue's
        // actual length depends on TTS voice/speed/locale, not a hardcoded "2-4 seconds" guess.
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                        releaseFocus()
                    }
                }
            },
        )
    }

    override fun speak(text: String) {
        scope.launch {
            val file = synthesizer.synthesizeToFile(text) ?: return@launch
            requestFocus()
            player.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            player.prepare()
            player.play()
        }
    }

    private fun requestFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun releaseFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    override fun shutdown() {
        releaseFocus()
        synthesizer.shutdown()
    }
}
