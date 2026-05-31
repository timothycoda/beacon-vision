package com.beacon.app.emergency

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.beacon.core.log.BeaconLog

object DialerIntentLauncher {
    fun openDialer(context: Context, phoneNumber: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            BeaconLog.w("DialerIntentLauncher", "No dialer: ${e.message}")
            false
        }
    }
}
