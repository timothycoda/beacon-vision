package com.beacon.core.log

import android.util.Log

/**
 * Thin logging facade. Centralised so we can later route logs to the
 * Diagnostics screen or disable them in release builds without touching callers.
 */
object BeaconLog {
    private const val GLOBAL_TAG = "Beacon"

    fun d(tag: String, message: String) = Log.d(GLOBAL_TAG, "[$tag] $message")
    fun i(tag: String, message: String) = Log.i(GLOBAL_TAG, "[$tag] $message")
    fun w(tag: String, message: String) = Log.w(GLOBAL_TAG, "[$tag] $message")
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        Log.e(GLOBAL_TAG, "[$tag] $message", throwable)
}
