package com.beacon.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Central, version-aware permission definitions. Permissions are requested
 * progressively (only when a feature needs them), never all at once on launch.
 */
object BeaconPermissions {

    /** Permissions required to scan for and connect to the glasses. */
    fun blePermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Pre-12 needs location to perform a BLE scan.
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    /** Notification permission (Android 13+), or null if not applicable. */
    fun notificationPermission(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    /** Location for emergency messages (all API levels we support). */
    fun emergencyLocationPermissions(): List<String> =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasEmergencyLocationPermission(context: Context): Boolean =
        emergencyLocationPermissions().all { isGranted(context, it) }

    /** Microphone, required for hands-free voice commands. */
    fun microphonePermission(): String = Manifest.permission.RECORD_AUDIO

    fun hasMicrophonePermission(context: Context): Boolean =
        isGranted(context, microphonePermission())

    /** Send SMS without opening the messaging app (hands-free emergency). */
    fun sendSmsPermission(): String = Manifest.permission.SEND_SMS

    fun hasSendSmsPermission(context: Context): Boolean =
        isGranted(context, sendSmsPermission())

    fun hasBlePermissions(context: Context): Boolean =
        blePermissions().all { isGranted(context, it) }

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
