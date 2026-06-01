package com.beacon.app.assistant

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
import com.beacon.app.MainActivity
import com.beacon.app.R
import com.beacon.app.emergency.SmsAutoSender
import com.beacon.app.emergency.SmsIntentLauncher
import com.beacon.app.emergency.SmsLaunchResult
import com.beacon.app.emergency.SmsSendResult
import com.beacon.app.walking.WalkingGuidanceService
import com.beacon.core.haptics.Haptics
import com.beacon.core.log.BeaconLog
import com.beacon.core.result.OperationResult
import com.beacon.data.emergency.EmergencyPreferences
import com.beacon.domain.emergency.EmergencyAlertDraft
import com.beacon.domain.emergency.EmergencyRepository
import com.beacon.domain.history.HistoryEntry
import com.beacon.domain.history.HistoryRepository
import com.beacon.domain.history.HistoryType
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.speech.Speaker
import com.beacon.domain.text.usecase.ReadTextUseCase
import com.beacon.domain.vision.usecase.CapturePhotoUseCase
import com.beacon.domain.vision.usecase.DescribeSceneUseCase
import com.beacon.domain.voice.SpeechToText
import com.beacon.domain.voice.VoiceCommand
import com.beacon.domain.voice.VoiceCommandMatcher
import com.beacon.domain.voice.VoiceError
import com.beacon.domain.voice.VoiceListeningState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

/**
 * Always-on hands-free assistant. Runs as a foreground service so the glasses
 * BLE link and the device-notify listener stay alive with the screen locked or
 * the app closed. When the glasses report their voice trigger (long-press on the
 * temple), we capture a single spoken command and run it headlessly — speaking
 * the answer aloud — so the user never has to touch the phone.
 */
@AndroidEntryPoint
class BeaconAssistantService : Service() {

    @Inject lateinit var glassesRepository: GlassesRepository
    @Inject lateinit var emergencyRepository: EmergencyRepository
    @Inject lateinit var emergencyPreferences: EmergencyPreferences
    @Inject lateinit var historyRepository: HistoryRepository
    @Inject lateinit var speechToText: SpeechToText
    @Inject lateinit var speaker: Speaker
    @Inject lateinit var haptics: Haptics
    @Inject lateinit var capturePhoto: CapturePhotoUseCase
    @Inject lateinit var describeScene: DescribeSceneUseCase
    @Inject lateinit var readText: ReadTextUseCase

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val handling = Mutex()
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAssistant()
                return START_NOT_STICKY
            }
            else -> startAssistant()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        speaker.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startAssistant() {
        if (started) return
        started = true
        ensureChannel()
        startForegroundCompat()
        BeaconLog.i(TAG, "Beacon assistant started")
        serviceScope.launch {
            glassesRepository.voiceTriggers.collect {
                onGlassesTrigger()
            }
        }
    }

    private fun stopAssistant() {
        BeaconLog.i(TAG, "Beacon assistant stopping")
        speaker.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        started = false
    }

    private fun onGlassesTrigger() {
        // Single-flight: ignore a new trigger while one is being handled.
        if (handling.isLocked) {
            BeaconLog.d(TAG, "trigger ignored — already handling a command")
            return
        }
        serviceScope.launch {
            if (!handling.tryLock()) return@launch
            try {
                handleTrigger()
            } finally {
                handling.unlock()
            }
        }
    }

    private suspend fun handleTrigger() {
        BeaconLog.i(TAG, "handling glasses voice trigger")
        haptics.confirm()
        // Speak a short cue, then wait for it to finish so it is not picked up by
        // the microphone, before we start listening.
        speaker.speak(getString(R.string.assistant_listening_prompt))
        waitUntilSpoken()

        val transcript = listenOnce()
        if (transcript == null) {
            speaker.speak("I didn't catch that. Long press your glasses and try again.")
            return
        }
        BeaconLog.i(TAG, "heard: \"$transcript\"")
        execute(VoiceCommandMatcher.match(transcript), transcript)
    }

    /** Drives one listening session, returning the final transcript or null. */
    private suspend fun listenOnce(): String? {
        var result: String? = null
        speechToText.listenOnce().collect { state ->
            when (state) {
                is VoiceListeningState.Result -> result = state.transcript
                    is VoiceListeningState.Error -> {
                    when (state.reason) {
                        VoiceError.PERMISSION_DENIED ->
                            speaker.speak("Microphone permission is needed for voice commands.")
                        VoiceError.BUSY ->
                            speaker.speak("The microphone is busy. Try again in a moment.")
                        VoiceError.NETWORK ->
                            speaker.speak("Speech recognition needs a moment. Please try again.")
                        VoiceError.UNAVAILABLE ->
                            speaker.speak("Voice commands are not available on this device.")
                        VoiceError.NO_MATCH, VoiceError.NO_SPEECH -> Unit
                        VoiceError.UNKNOWN ->
                            speaker.speak("Something went wrong. Long press your glasses and try again.")
                    }
                    result = null
                }
                else -> Unit
            }
        }
        return result
    }

    private suspend fun execute(command: VoiceCommand, transcript: String) {
        when (command) {
            VoiceCommand.WhatIsAhead -> runWhatIsAhead()
            VoiceCommand.ReadText -> runReadText()
            VoiceCommand.WalkingMode -> {
                speaker.speak("Starting walking mode.")
                WalkingGuidanceService.start(this)
            }
            VoiceCommand.GlassesStatus -> speakStatus()
            VoiceCommand.StopSpeaking -> speaker.stop()
            VoiceCommand.Emergency -> runEmergency()
            VoiceCommand.VoiceSettings -> openApp(ROUTE_VOICE_SETTINGS, "Opening voice settings.")
            VoiceCommand.GoHome -> openApp(ROUTE_HOME, null)
            is VoiceCommand.Unknown -> speaker.speak(
                "Sorry, I didn't understand $transcript. Try: what is ahead, read this, " +
                    "walking mode, or emergency.",
            )
        }
    }

    private suspend fun runWhatIsAhead() {
        if (!glassesRepository.isConnected()) {
            speaker.speak("Your glasses are not connected.")
            return
        }
        speaker.speak("Looking ahead. One moment.")
        when (val capture = capturePhoto()) {
            is OperationResult.Failure -> speaker.speak(capture.message)
            is OperationResult.Success -> when (val described = describeScene(capture.value)) {
                is OperationResult.Failure -> speaker.speak(described.message)
                is OperationResult.Success -> speaker.speak(described.value.spokenSummary)
            }
        }
    }

    private suspend fun runReadText() {
        if (!glassesRepository.isConnected()) {
            speaker.speak("Your glasses are not connected.")
            return
        }
        speaker.speak("Reading text. One moment.")
        when (val capture = capturePhoto()) {
            is OperationResult.Failure -> speaker.speak(capture.message)
            is OperationResult.Success -> when (val recognized = readText(capture.value)) {
                is OperationResult.Failure -> speaker.speak(recognized.message)
                is OperationResult.Success -> speaker.speak(recognized.value.spokenText)
            }
        }
    }

    private suspend fun runEmergency() {
        speaker.speak("Emergency. Preparing your message. One moment.")
        haptics.error()
        val settings = emergencyPreferences.settings.first()
        when (val prepared = emergencyRepository.prepareAlert(settings.includeSceneFromGlasses)) {
            is OperationResult.Failure -> speaker.speak(prepared.message)
            is OperationResult.Success -> {
                val draft = prepared.value
                if (!settings.autoSendSms) {
                    speaker.speak("Opening your messaging app. Review and tap send.")
                    fallbackOpenSms(draft)
                    return
                }
                if (settings.confirmBeforeSend) {
                    speaker.speak("Sending emergency message in 3 seconds.")
                    delay(CONFIRM_SEND_DELAY_MS)
                }
                when (val sent = SmsAutoSender.send(this, draft)) {
                    SmsSendResult.Success -> {
                        historyRepository.add(
                            HistoryEntry(
                                type = HistoryType.EMERGENCY,
                                summary = "Emergency SMS sent hands-free.",
                            ),
                        )
                        speaker.speak("Emergency message sent to your trusted contact.")
                    }
                    SmsSendResult.PermissionDenied -> {
                        speaker.speak(
                            "Allow send SMS in Beacon emergency settings for automatic sending. " +
                                "Opening your messaging app instead.",
                        )
                        fallbackOpenSms(draft)
                    }
                    is SmsSendResult.Failed -> {
                        speaker.speak(
                            "${sent.reason} Opening your messaging app so you can send manually.",
                        )
                        fallbackOpenSms(draft)
                    }
                }
            }
        }
    }

    private fun fallbackOpenSms(draft: EmergencyAlertDraft) {
        when (SmsIntentLauncher.launch(this, draft)) {
            is SmsLaunchResult.Success -> Unit
            is SmsLaunchResult.NoSmsApp ->
                speaker.speak("No messaging app found. Install Messages or send manually.")
            is SmsLaunchResult.Failed ->
                speaker.speak("Could not open your messaging app.")
        }
    }

    private fun speakStatus() {
        val battery = glassesRepository.battery.value
        val connected = glassesRepository.isConnected()
        val message = when {
            !connected -> "Your glasses are not connected."
            battery != null -> "Glasses connected. Battery ${battery.levelPercent} percent."
            else -> "Glasses connected."
        }
        speaker.speak(message)
    }

    private fun openApp(route: String, spoken: String?) {
        spoken?.let { speaker.speak(it) }
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        runCatching { startActivity(intent) }
            .onFailure { BeaconLog.w(TAG, "could not open app for route=$route") }
    }

    /** Suspends until the current utterance has finished (bounded), so TTS does
     * not leak into the microphone. */
    private suspend fun waitUntilSpoken() {
        runCatching {
            // Give TTS a moment to start, then wait for it to go idle so the cue
            // is not picked up by the microphone.
            delay(250)
            kotlinx.coroutines.withTimeoutOrNull(CUE_MAX_MS) {
                speaker.isSpeaking.first { speaking -> !speaking }
            }
        }
        delay(150)
    }

    private fun startForegroundCompat() {
        // Rely on the manifest-declared foregroundServiceType (connectedDevice|
        // microphone), matching the proven WalkingGuidanceService pattern.
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.assistant_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.assistant_notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BeaconAssistantService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.assistant_notification_title))
            .setContentText(getString(R.string.assistant_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(0, getString(R.string.assistant_notification_stop), stopIntent)
            .build()
    }

    companion object {
        private const val TAG = "BeaconAssistantService"
        const val ACTION_START = "com.beacon.app.assistant.START"
        const val ACTION_STOP = "com.beacon.app.assistant.STOP"
        private const val CHANNEL_ID = "beacon_assistant"
        private const val NOTIFICATION_ID = 7
        private const val CUE_MAX_MS = 2_500L
        private const val CONFIRM_SEND_DELAY_MS = 3_000L

        const val ROUTE_EMERGENCY = "emergency"
        const val ROUTE_VOICE_SETTINGS = "voice_settings"
        const val ROUTE_HOME = "home"

        fun start(context: Context) {
            val intent = Intent(context, BeaconAssistantService::class.java)
                .setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BeaconAssistantService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
