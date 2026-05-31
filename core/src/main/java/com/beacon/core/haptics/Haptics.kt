package com.beacon.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Simple haptic confirmations for key, accessibility-critical actions
 * (connect succeeded, error, etc.). UI-level taps use Compose haptics.
 */
class Haptics(private val context: Context) {

    private val vibrator: Vibrator?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun confirm() = vibrate(60)
    fun error() = vibrate(200)
    fun emergencyPulse() {
        runCatching {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            val pattern = longArrayOf(0, 120, 80, 200)
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun vibrate(durationMs: Long) {
        // Never let a missing VIBRATE permission or an OEM vibrator quirk crash a
        // feature (e.g. emergency). Haptics are a nice-to-have, not critical.
        runCatching {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
