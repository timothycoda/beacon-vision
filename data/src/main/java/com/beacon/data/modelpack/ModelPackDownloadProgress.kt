package com.beacon.data.modelpack

import com.beacon.domain.modelpack.ModelPackId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ModelPackDownloadProgress(
    val packId: ModelPackId,
    val displayName: String,
    val bytesDownloaded: Long,
    val sizeBytes: Long,
) {
    val percent: Int?
        get() = if (sizeBytes > 0) {
            ((bytesDownloaded * 100L) / sizeBytes).toInt().coerceIn(0, 100)
        } else {
            null
        }
}

@Singleton
class ModelPackDownloadProgressBus @Inject constructor() {
    private val _active = MutableStateFlow<ModelPackDownloadProgress?>(null)
    val active: StateFlow<ModelPackDownloadProgress?> = _active.asStateFlow()

    fun update(progress: ModelPackDownloadProgress) {
        _active.value = progress
    }

    fun clear() {
        _active.value = null
    }
}
