package com.beacon.app.emergency

import android.content.Context
import com.beacon.app.R
import com.beacon.core.haptics.Haptics
import com.beacon.core.result.OperationResult
import com.beacon.data.phone.PhoneModeSessionController
import com.beacon.domain.helper.TrustedHelper
import com.beacon.domain.location.LocationProvider
import com.beacon.domain.speech.Speaker
import com.beacon.domain.vision.LatestImageProvider
import com.beacon.domain.whatsapp.WhatsAppHelperIntentBuilder
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
            template = context.getString(R.string.handoff_call_template),
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
                speaker.speak(context.getString(R.string.handoff_success_call))
            else -> speaker.speak(context.getString(R.string.handoff_opening_whatsapp))
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
            template = context.getString(R.string.handoff_message_template),
            lastSceneSummary = latestImage.latestSceneSummary(),
            locationMapsUrl = location,
        )
        speaker.speak(context.getString(R.string.handoff_opening_message))
        haptics.confirm()
        return WhatsAppIntentLauncher.openTextChat(context, helper.phoneNumber, message)
    }

    fun onReturnedFromWhatsApp(context: Context): Boolean {
        val resumed = sessionController.tryResumeAfterWhatsAppReturn()
        if (resumed) {
            speaker.speak(context.getString(R.string.handoff_resume_tts))
        }
        return resumed
    }
}
