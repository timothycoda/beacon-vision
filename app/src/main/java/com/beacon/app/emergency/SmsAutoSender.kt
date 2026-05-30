package com.beacon.app.emergency

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import com.beacon.core.log.BeaconLog
import com.beacon.core.permission.BeaconPermissions
import com.beacon.domain.emergency.EmergencyAlertDraft

/** Result of sending an emergency SMS directly (no user tap on Send). */
sealed interface SmsSendResult {
    data object Success : SmsSendResult
    data object PermissionDenied : SmsSendResult
    data class Failed(val reason: String) : SmsSendResult
}

/**
 * Sends an emergency SMS in the background using [SmsManager]. Requires
 * [android.Manifest.permission.SEND_SMS]. Used for hands-free "emergency"
 * voice commands when the screen is locked.
 */
object SmsAutoSender {

    private const val TAG = "SmsAutoSender"

    fun send(context: Context, draft: EmergencyAlertDraft): SmsSendResult {
        if (!BeaconPermissions.hasSendSmsPermission(context)) {
            return SmsSendResult.PermissionDenied
        }
        val phone = normalizeEmergencyPhone(draft.phoneNumber)
        if (phone.isBlank()) {
            return SmsSendResult.Failed("Phone number is not valid.")
        }

        val smsManager = resolveSmsManager(context)
            ?: return SmsSendResult.Failed("SMS is not available on this device.")

        return try {
            val parts = smsManager.divideMessage(draft.message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phone, null, draft.message, null, null)
            }
            BeaconLog.i(TAG, "Emergency SMS sent to $phone (${parts.size} part(s))")
            SmsSendResult.Success
        } catch (e: SecurityException) {
            BeaconLog.w(TAG, "SEND_SMS denied: ${e.message}")
            SmsSendResult.PermissionDenied
        } catch (e: Exception) {
            BeaconLog.e(TAG, "send failed", e)
            SmsSendResult.Failed(e.message ?: "Could not send the message.")
        }
    }

    private fun resolveSmsManager(context: Context): SmsManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
}
