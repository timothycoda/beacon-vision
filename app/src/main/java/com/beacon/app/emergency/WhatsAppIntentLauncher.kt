package com.beacon.app.emergency

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.beacon.core.log.BeaconLog
import com.beacon.domain.whatsapp.WhatsAppHelperIntentBuilder
import java.io.File

sealed interface WhatsAppLaunchResult {
    data object Success : WhatsAppLaunchResult
    data object WhatsAppNotInstalled : WhatsAppLaunchResult
    data class Failed(val reason: String) : WhatsAppLaunchResult
}

object WhatsAppIntentLauncher {

    private const val TAG = "WhatsAppIntentLauncher"
    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    fun isWhatsAppInstalled(context: Context): Boolean =
        whatsAppPackage(context) != null

    fun whatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        return listOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE).firstOrNull { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)
        }
    }

    fun openTextChat(context: Context, phoneNumber: String, message: String): WhatsAppLaunchResult {
        val url = WhatsAppHelperIntentBuilder.buildWaMeUrl(phoneNumber, message)
            ?: return WhatsAppLaunchResult.Failed("Phone number is not valid for WhatsApp.")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        whatsAppPackage(context)?.let { intent.setPackage(it) }
        return startExternal(context, intent, waMeHandoff = true)
    }

    fun shareImageAndText(
        context: Context,
        imageFile: File,
        message: String,
    ): WhatsAppLaunchResult {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, imageFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (isWhatsAppInstalled(context)) {
            intent.setPackage(WHATSAPP_PACKAGE)
        }
        return startExternal(context, intent, waMeHandoff = false)
    }

    fun shareTextOnly(context: Context, message: String): WhatsAppLaunchResult {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        if (isWhatsAppInstalled(context)) {
            intent.setPackage(WHATSAPP_PACKAGE)
        }
        return startExternal(context, intent, waMeHandoff = false)
    }

    private fun startExternal(
        context: Context,
        intent: Intent,
        waMeHandoff: Boolean,
    ): WhatsAppLaunchResult {
        val launchContext = context.findActivity() ?: context
        if (launchContext !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val waInstalled = isWhatsAppInstalled(context)
        if (!waMeHandoff && !waInstalled) {
            return WhatsAppLaunchResult.WhatsAppNotInstalled
        }
        return try {
            if (waMeHandoff) {
                launchContext.startActivity(intent)
            } else {
                val chooser = Intent.createChooser(intent, "Share via").apply {
                    if (launchContext !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                launchContext.startActivity(chooser)
            }
            WhatsAppLaunchResult.Success
        } catch (_: ActivityNotFoundException) {
            if (waMeHandoff) {
                intent.setPackage(null)
                return try {
                    launchContext.startActivity(intent)
                    WhatsAppLaunchResult.Success
                } catch (_: ActivityNotFoundException) {
                    WhatsAppLaunchResult.WhatsAppNotInstalled
                }
            }
            WhatsAppLaunchResult.Failed("No app can share this message.")
        } catch (e: Exception) {
            BeaconLog.e(TAG, "WhatsApp launch failed", e)
            WhatsAppLaunchResult.Failed(e.message ?: "Could not open WhatsApp.")
        }
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
