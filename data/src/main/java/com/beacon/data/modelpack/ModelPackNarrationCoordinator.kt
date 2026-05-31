package com.beacon.data.modelpack

/**
 * Notified when a Gemma pack is installed or removed so the app can load/unload LiteRT-LM.
 */
interface ModelPackNarrationCoordinator {
    fun onPackInstalled()
    fun onPackRemoved()
}
