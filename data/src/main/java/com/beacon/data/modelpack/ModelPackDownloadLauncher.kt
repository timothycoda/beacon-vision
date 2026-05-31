package com.beacon.data.modelpack

import com.beacon.domain.modelpack.ModelPackId

/**
 * Starts a foreground download so Android does not kill multi‑GB transfers.
 * Implemented in the app module (service + notification).
 */
interface ModelPackDownloadLauncher {
    fun startForegroundDownload(id: ModelPackId)
    fun stopForegroundDownload()
}
