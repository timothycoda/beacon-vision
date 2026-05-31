package com.beacon.core.debug

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Session debug trace: Logcat tag [BeaconDebug] + NDJSON on device at
 * files/debug-b98d12.log (pull with scripts/pull-debug-logs.sh).
 */
object DebugTrace {
    private const val SESSION_ID = "b98d12"
    private const val LOG_FILE = "debug-b98d12.log"
    private const val TAG = "BeaconDebug"

    @Volatile
    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.applicationContext.filesDir, LOG_FILE)
        clear()
    }

    fun clear() {
        runCatching { logFile?.delete() }
    }

    fun event(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "pre-fix",
    ) {
        val payload = JSONObject()
            .put("sessionId", SESSION_ID)
            .put("hypothesisId", hypothesisId)
            .put("location", location)
            .put("message", message)
            .put("timestamp", System.currentTimeMillis())
            .put("runId", runId)
        val dataObj = JSONObject()
        data.forEach { (k, v) -> dataObj.put(k, v) }
        payload.put("data", dataObj)
        val line = payload.toString()
        Log.i(TAG, line)
        // #region agent log
        runCatching { logFile?.appendText("$line\n") }
        // #endregion
    }
}
