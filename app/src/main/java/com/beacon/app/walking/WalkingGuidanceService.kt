package com.beacon.app.walking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.beacon.app.MainActivity
import com.beacon.app.R
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.speech.Speaker
import com.beacon.domain.vision.usecase.CapturePhotoUseCase
import com.beacon.domain.vision.usecase.DescribeSceneUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for walking mode: periodic capture + short scene cues while
 * the user walks. Stops via notification action or the Walking mode screen.
 */
@AndroidEntryPoint
class WalkingGuidanceService : Service() {

    @Inject lateinit var capturePhoto: CapturePhotoUseCase
    @Inject lateinit var describeScene: DescribeSceneUseCase
    @Inject lateinit var speaker: Speaker
    @Inject lateinit var glassesRepository: GlassesRepository
    @Inject lateinit var dispatchers: DispatcherProvider

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var guidanceJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startGuidance()
            ACTION_STOP -> stopGuidance()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        guidanceJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startGuidance() {
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(running = true))
        guidanceJob?.cancel()
        guidanceJob = serviceScope.launch(dispatchers.io) {
            BeaconLog.i(TAG, "Walking mode started")
            while (isActive) {
                if (!glassesRepository.isConnected()) {
                    delay(NOT_CONNECTED_RETRY_MS)
                    continue
                }
                runGuidanceTick()
                delay(GUIDANCE_INTERVAL_MS)
            }
        }
    }

    private suspend fun runGuidanceTick() {
        when (val capture = capturePhoto()) {
            is OperationResult.Failure -> {
                BeaconLog.w(TAG, "walking capture failed: ${capture.message}")
            }
            is OperationResult.Success -> {
                when (val described = describeScene(capture.value)) {
                    is OperationResult.Failure ->
                        speaker.speak(described.message, interrupt = true)
                    is OperationResult.Success ->
                        speaker.speak(described.value.spokenSummary, interrupt = true)
                }
            }
        }
    }

    private fun stopGuidance() {
        guidanceJob?.cancel()
        guidanceJob = null
        speaker.stop()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        BeaconLog.i(TAG, "Walking mode stopped")
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.walking_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.walking_notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(running: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WalkingGuidanceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.walking_notification_title))
            .setContentText(
                if (running) getString(R.string.walking_notification_running)
                else getString(R.string.walking_notification_stopped),
            )
            .setContentIntent(openApp)
            .setOngoing(running)
            .addAction(0, getString(R.string.walking_notification_stop), stopIntent)
            .build()
    }

    companion object {
        private const val TAG = "WalkingGuidanceService"
        const val ACTION_START = "com.beacon.app.walking.START"
        const val ACTION_STOP = "com.beacon.app.walking.STOP"
        private const val CHANNEL_ID = "beacon_walking"
        private const val NOTIFICATION_ID = 42
        private const val GUIDANCE_INTERVAL_MS = 12_000L
        private const val NOT_CONNECTED_RETRY_MS = 3_000L

        fun start(context: Context) {
            val intent = Intent(context, WalkingGuidanceService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, WalkingGuidanceService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
