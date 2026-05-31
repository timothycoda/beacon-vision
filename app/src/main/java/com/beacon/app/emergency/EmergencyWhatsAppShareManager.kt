package com.beacon.app.emergency

import android.content.Context
import com.beacon.core.haptics.Haptics
import com.beacon.core.result.OperationResult
import com.beacon.data.emergency.EmergencyImageCompressor
import com.beacon.domain.emergency.EmergencyLocation
import com.beacon.domain.emergency.EmergencySettings
import com.beacon.domain.helper.TrustedHelper
import com.beacon.domain.location.LocationProvider
import com.beacon.domain.speech.Speaker
import com.beacon.domain.vision.LatestImageProvider
import com.beacon.domain.whatsapp.WhatsAppEmergencyMessageBuilder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class EmergencySharePreview(
    val message: String,
    val hasImage: Boolean,
    val hasLocation: Boolean,
    val contactCount: Int,
)

@Singleton
class EmergencyWhatsAppShareManager @Inject constructor(
    private val latestImage: LatestImageProvider,
    private val locationProvider: LocationProvider,
    private val imageCompressor: EmergencyImageCompressor,
    private val speaker: Speaker,
    private val haptics: Haptics,
) {

    suspend fun buildPreview(
        settings: EmergencySettings,
        contacts: List<TrustedHelper>,
    ): EmergencySharePreview {
        val location = if (settings.includeLocationInEmergencyAlerts) {
            when (val loc = locationProvider.getLastKnown()) {
                is OperationResult.Success -> loc.value
                is OperationResult.Failure -> null
            }
        } else {
            null
        }
        val scene = latestImage.latestSceneSummary()
        val message = WhatsAppEmergencyMessageBuilder.build(
            location = location,
            sceneSummary = scene,
        )
        val hasImage = settings.includeLatestImageInEmergencyAlerts &&
            latestImage.latestJpegBytes() != null
        return EmergencySharePreview(
            message = message,
            hasImage = hasImage,
            hasLocation = location != null,
            contactCount = contacts.size,
        )
    }

    suspend fun shareToWhatsApp(
        context: Context,
        settings: EmergencySettings,
        contact: TrustedHelper,
    ): WhatsAppLaunchResult {
        speaker.speak(
            "I will prepare your emergency message with your latest image and location. " +
                "WhatsApp will open for you to send it to your trusted contact.",
        )
        haptics.emergencyPulse()

        val location: EmergencyLocation? = if (settings.includeLocationInEmergencyAlerts) {
            when (val loc = locationProvider.getLastKnown()) {
                is OperationResult.Success -> loc.value
                is OperationResult.Failure -> null
            }
        } else {
            null
        }
        if (location == null) {
            speaker.speak("Location is not available. I will send the image and emergency message only.")
        }

        val message = WhatsAppEmergencyMessageBuilder.build(
            location = location,
            sceneSummary = latestImage.latestSceneSummary(),
        )

        val jpeg = if (settings.includeLatestImageInEmergencyAlerts) {
            latestImage.latestJpegBytes()
        } else {
            null
        }

        if (jpeg == null) {
            speaker.speak("No latest image is available. I will send text and location only.")
            return WhatsAppIntentLauncher.openTextChat(context, contact.phoneNumber, message)
        }

        val file: File? = imageCompressor.compressToShareFile(jpeg)
        if (file == null) {
            return WhatsAppIntentLauncher.openTextChat(context, contact.phoneNumber, message)
        }

        val imageResult = WhatsAppIntentLauncher.shareImageAndText(context, file, message)
        if (imageResult is WhatsAppLaunchResult.Success) {
            return imageResult
        }
        return WhatsAppIntentLauncher.openTextChat(context, contact.phoneNumber, message)
    }
}
