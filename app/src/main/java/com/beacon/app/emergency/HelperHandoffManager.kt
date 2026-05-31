package com.beacon.app.emergency

import android.content.Context
import com.beacon.core.haptics.Haptics
import com.beacon.data.phone.PhoneModeSessionController
import com.beacon.domain.helper.TrustedHelper
import com.beacon.domain.location.LocationProvider
import com.beacon.domain.speech.Speaker
import com.beacon.domain.vision.LatestImageProvider
import com.beacon.domain.whatsapp.WhatsAppHelperIntentBuilder
import com.beacon.core.result.OperationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HelperHandoffManager @Inject constructor(
    private val sessionController: PhoneModeSessionController,
    private val latestImage: LatestImageProvider,
    private val locationProvider: LocationProvider,
    private val speaker: Speaker,
    private val haptics: Haptics,
) {
    suspend fun openCallHelperOnWhatsApp(
        context: Context,
        helper: TrustedHelper,
        pauseCamera: Boolean,
    ): WhatsAppLaunchResult {
        val location = when (val loc = locationProvider.getLastKnown()) {
            is OperationResult.Success -> loc.value.mapsUrl
            is OperationResult.Failure -> null
        }
        val message = WhatsAppHelperIntentBuilder.buildHelperMessage(
            template = HELPER_CALL_TEMPLATE,
            lastSceneSummary = latestImage.latestSceneSummary(),
            locationMapsUrl = location,
        )
        haptics.confirm()
        if (pauseCamera) {
            sessionController.pauseForWhatsAppHandoff()
        }
        val result = WhatsAppIntentLauncher.openTextChat(context, helper.phoneNumber, message)
        when (result) {
            WhatsAppLaunchResult.Success ->
                speaker.speak("WhatsApp is open. Ask your helper to start a video call when ready.")
            else -> speaker.speak(PRE_HANDOFF_TTS)
        }
        return result
    }

    suspend fun openMessageHelper(
        context: Context,
        helper: TrustedHelper,
    ): WhatsAppLaunchResult {
        val location = when (val loc = locationProvider.getLastKnown()) {
            is OperationResult.Success -> loc.value.mapsUrl
            is OperationResult.Failure -> null
        }
        val message = WhatsAppHelperIntentBuilder.buildHelperMessage(
            template = HELPER_MESSAGE_TEMPLATE,
            lastSceneSummary = latestImage.latestSceneSummary(),
            locationMapsUrl = location,
        )
        speaker.speak("Opening WhatsApp to message your helper.")
        haptics.confirm()
        return WhatsAppIntentLauncher.openTextChat(context, helper.phoneNumber, message)
    }

    fun onReturnedFromWhatsApp(): Boolean {
        val resumed = sessionController.tryResumeAfterWhatsAppReturn()
        if (resumed) {
            speaker.speak(RESUME_TTS)
        }
        return resumed
    }

    private companion object {
        const val PRE_HANDOFF_TTS =
            "Opening WhatsApp. Point your phone camera forward so your helper can see what is ahead. " +
                "Beacon will pause camera detection during the call and resume when you return."
        const val RESUME_TTS = "Beacon is back. Camera guidance resumed."
        const val HELPER_CALL_TEMPLATE =
            "Hello, I need visual assistance on Beacon. Please start a WhatsApp video call with me " +
                "and guide me. I will point my phone camera forward so you can see what is ahead. " +
                "{lastSceneSummaryText} {locationText}"
        const val HELPER_MESSAGE_TEMPLATE =
            "Hello, I need visual assistance on Beacon. Please message me when you can. " +
                "{lastSceneSummaryText} {locationText}"
    }
}
