package com.stride.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.stride.app.MainActivity
import com.stride.app.R
import com.stride.app.run.RunSessionEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * The real Phase 4 counterpart to [com.stride.app.run.RunSessionService]: that service keeps GPS
 * and the *process* alive (`foregroundServiceType="location"`); this one is what actually owns
 * an active [MediaSession] so cue playback gets legitimate `mediaPlayback`-type foreground
 * service treatment and system media-session integration (lock screen, "playing" indicator),
 * instead of the interim engine's TTS speaking with no real media session behind it at all. Two
 * services, not one, because `Service` and `MediaSessionService` can't be multiply inherited —
 * see the manifest's `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission comment, which anticipated
 * exactly this split.
 *
 * The [ExoPlayer] itself is an app-scoped Hilt singleton (see [AudioModule]), not owned by this
 * service — [Media3VoiceCueSpeaker] injects the same instance directly to control playback,
 * rather than this service exposing it through a companion-object singleton hack.
 *
 * Calls [ServiceCompat.startForeground] explicitly in [onCreate] rather than relying on Media3's
 * own internal notification manager to promote it automatically — found by actually running this
 * on-device: `Context.startForegroundService()` requires `Service.startForeground()` within a few
 * seconds or the OS kills the app with `ForegroundServiceDidNotStartInTimeException`, and the
 * automatic MediaSessionService promotion (tied to the player reaching an active playback state)
 * wasn't reliably fast enough — a cue doesn't necessarily start playing the instant this service
 * is created.
 */
@AndroidEntryPoint
class RunAudioService : MediaSessionService() {

    @Inject lateinit var player: ExoPlayer

    @Inject lateinit var engine: RunSessionEngine

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .build()

        // Mirrors RunSessionService's own isFinished-stops-self pattern — no independent
        // lifecycle policy to keep in sync by hand.
        observeJob = engine.state
            .onEach { state -> if (state.isFinished || state.isCancelled) stopSelf() }
            .launchIn(serviceScope)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        observeJob?.cancel()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        // player itself is an app-scoped singleton (see AudioModule) — released with the
        // process, not torn down per-run the way this service's own lifecycle is.
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stride")
            .setContentText("Voice cues active")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Voice cues", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while voice cues are active during a run."
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "stride_run_audio"
        private const val NOTIFICATION_ID = 4202

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RunAudioService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RunAudioService::class.java))
        }
    }
}
