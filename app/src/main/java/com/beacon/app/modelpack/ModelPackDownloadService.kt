package com.beacon.app.modelpack

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.beacon.app.MainActivity
import com.beacon.app.R
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.data.modelpack.ModelPackCatalog
import com.beacon.data.modelpack.ModelPackDownloadProgressBus
import com.beacon.data.modelpack.ModelPackRepositoryImpl
import com.beacon.domain.modelpack.ModelPackId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ModelPackDownloadService : Service() {

    @Inject lateinit var repository: ModelPackRepositoryImpl
    @Inject lateinit var dispatchers: DispatcherProvider
    @Inject lateinit var downloadProgressBus: ModelPackDownloadProgressBus

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var downloadJob: Job? = null
    private var progressJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packId = intent?.getStringExtra(EXTRA_PACK_ID)?.let { key ->
            ModelPackId.fromStorageKey(key)
        }
        if (packId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val entry = ModelPackCatalog.find(packId)
        val title = entry?.displayName ?: "AI pack"
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(title, percent = null, indeterminate = true))

        progressJob?.cancel()
        progressJob = serviceScope.launch(dispatchers.main) {
            downloadProgressBus.active.collect { progress ->
                if (progress == null || progress.packId != packId) return@collect
                val manager = getSystemService(NotificationManager::class.java) ?: return@collect
                manager.notify(
                    NOTIFICATION_ID,
                    buildNotification(
                        title = progress.displayName,
                        percent = progress.percent,
                        indeterminate = progress.percent == null,
                    ),
                )
            }
        }

        downloadJob?.cancel()
        downloadJob = serviceScope.launch(dispatchers.io) {
            BeaconLog.i(TAG, "Starting foreground download for $packId")
            repository.performDownload(packId)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        downloadJob?.cancel()
        progressJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(title: String, percent: Int?, indeterminate: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading $title")
            .setContentText(
                if (percent != null) "Download $percent% — tap to open Beacon"
                else "Downloading model pack…",
            )
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        when {
            indeterminate -> builder.setProgress(0, 0, true)
            percent != null -> builder.setProgress(100, percent.coerceIn(0, 100), false)
        }
        return builder.build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model downloads",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Shows progress while offline AI packs download"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "ModelPackDownloadSvc"
        private const val CHANNEL_ID = "model_pack_download"
        private const val NOTIFICATION_ID = 42
        private const val EXTRA_PACK_ID = "pack_id"

        fun intent(context: Context, id: ModelPackId?): Intent =
            Intent(context, ModelPackDownloadService::class.java).apply {
                if (id != null) putExtra(EXTRA_PACK_ID, id.storageKey)
            }
    }
}
