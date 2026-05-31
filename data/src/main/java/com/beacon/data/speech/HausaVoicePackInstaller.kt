package com.beacon.data.speech

import android.content.Context
import com.beacon.core.log.BeaconLog
import com.beacon.data.modelpack.ModelPackStorage
import com.beacon.domain.modelpack.ModelPackId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ensures the Hausa MMS ONNX pack directory contains [tokens.txt] alongside [model.onnx].
 * Tokens ship in APK assets (~300 bytes); the ONNX file is downloaded by the user.
 */
@Singleton
class HausaVoicePackInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: ModelPackStorage,
) {
    fun ensurePackLayout(): Boolean {
        val dir = storage.packDir(ModelPackId.HAUSA_VOICE)
        val model = File(dir, MODEL_ONNX)
        val tokens = File(dir, TOKENS_FILE)
        if (!model.isFile || model.length() <= 0L) return false

        if (!tokens.isFile || tokens.length() <= 0L) {
            runCatching {
                context.assets.open(ASSET_TOKENS).use { input ->
                    tokens.outputStream().use { output -> input.copyTo(output) }
                }
            }.onFailure {
                BeaconLog.e(TAG, "Failed to copy Hausa tokens.txt", it)
                return false
            }
        }
        return true
    }

    private companion object {
        const val TAG = "HausaVoicePackInstaller"
        const val ASSET_TOKENS = "hausa_mms/tokens.txt"
        const val MODEL_ONNX = "model.onnx"
        const val TOKENS_FILE = "tokens.txt"
    }
}
