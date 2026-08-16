package com.stride.app.run

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stride.app.MainActivity
import com.stride.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Exists for one reason: keep the app **process** alive while [RunSessionEngine] is doing real
 * work, so Android doesn't reclaim it the moment the app backgrounds (this is what actually
 * fixes "voice cues/GPS stop if I switch apps mid-run" — the engine itself already runs outside
 * ViewModel scope, but that alone doesn't stop the OS from killing the whole process under
 * memory pressure while backgrounded). Stops itself the moment the engine finishes; it does no
 * work of its own beyond observing state for the notification text.
 */
@AndroidEntryPoint
class RunSessionService : Service() {

    @Inject lateinit var engine: RunSessionEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification("Getting your session ready…"))

        observeJob = engine.state
            .onEach { state ->
                if (state.isFinished) {
                    stopSelf()
                    return@onEach
                }
                val label = state.currentStep?.let { step ->
                    val m = state.remainingSeconds / 60
                    val s = state.remainingSeconds % 60
                    "${step.stepType.name.lowercase().replaceFirstChar(Char::uppercase)} — %d:%02d".format(m, s)
                } ?: "Session in progress"
                updateNotification(label)
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observeJob?.cancel()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stride")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Active session", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while a run is in progress so cues and tracking keep working in the background."
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "stride_run_session"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context) {
            val intent = Intent(context, RunSessionService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RunSessionService::class.java))
        }
    }
}
