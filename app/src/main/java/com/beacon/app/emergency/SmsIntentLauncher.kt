package com.beacon.app.emergency

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.beacon.core.log.BeaconLog
import com.beacon.domain.emergency.EmergencyAlertDraft

/** Result of trying to open the system SMS/Messages app. */
sealed interface SmsLaunchResult {
    data object Success : SmsLaunchResult
    data object NoSmsApp : SmsLaunchResult
    data class Failed(val reason: String) : SmsLaunchResult
}

/**
 * Opens the system SMS app with a pre-filled message. The user must tap Send.
 * Tries several intent shapes for compatibility (including ColorOS / Oppo).
 */
object SmsIntentLauncher {

    private const val TAG = "SmsIntentLauncher"

    fun launch(context: Context, draft: EmergencyAlertDraft): SmsLaunchResult {
        val phone = normalizeEmergencyPhone(draft.phoneNumber)
        if (phone.isBlank()) {
            return SmsLaunchResult.Failed("Phone number is not valid.")
        }

        val launchContext = context.findActivity() ?: context
        val intents = buildIntents(phone, draft.message)

        for (intent in intents) {
            val resolved = intent.resolveActivity(launchContext.packageManager) != null
            if (!resolved) continue
            return try {
                if (launchContext !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                launchContext.startActivity(intent)
                BeaconLog.i(TAG, "SMS intent launched: ${intent.action}")
                SmsLaunchResult.Success
            } catch (e: ActivityNotFoundException) {
                BeaconLog.w(TAG, "No activity for ${intent.action}: ${e.message}")
                continue
            } catch (e: Exception) {
                BeaconLog.e(TAG, "startActivity failed for ${intent.action}", e)
                continue
            }
        }

        // Last resort: generic share sheet with SMS body (user picks Messages).
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, draft.message)
            if (launchContext !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(share, "Send emergency message").apply {
            if (launchContext !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            launchContext.startActivity(chooser)
            SmsLaunchResult.Success
        } catch (e: Exception) {
            BeaconLog.e(TAG, "share chooser failed", e)
            SmsLaunchResult.NoSmsApp
        }
    }

    private fun buildIntents(phone: String, message: String): List<Intent> {
        val encodedBody = Uri.encode(message)
        return listOf(
            // Preferred on many devices: body in the URI.
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone?body=$encodedBody")),
            // Classic: smsto URI + sms_body extra.
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                putExtra("sms_body", message)
            },
            // Some OEMs use sms: scheme.
            Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone?body=$encodedBody")),
            Intent(Intent.ACTION_SENDTO, Uri.parse("sms:$phone")).apply {
                putExtra("sms_body", message)
            },
        )
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
