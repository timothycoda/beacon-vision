package com.beacon.data.modelpack

import com.beacon.domain.modelpack.ModelPackDownloadFailure
import java.io.IOException

internal object ModelPackDownloadErrors {

    fun classify(throwable: Throwable): ModelPackDownloadFailure = when {
        throwable is IOException && throwable.message?.contains("HTTP", ignoreCase = true) == true ->
            ModelPackDownloadFailure.NETWORK
        throwable is IOException -> ModelPackDownloadFailure.NETWORK
        throwable.message?.contains("verification", ignoreCase = true) == true ->
            ModelPackDownloadFailure.VERIFICATION_FAILED
        else -> ModelPackDownloadFailure.UNKNOWN
    }

    fun userMessage(failure: ModelPackDownloadFailure, throwable: Throwable?): String =
        failure.userMessage(throwable?.message?.takeIf { failure == ModelPackDownloadFailure.UNKNOWN })
}
