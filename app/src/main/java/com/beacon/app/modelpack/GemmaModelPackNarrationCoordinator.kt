package com.beacon.app.modelpack

import com.beacon.app.vision.GemmaNarrationEngine
import com.beacon.data.modelpack.ModelPackNarrationCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemmaModelPackNarrationCoordinator @Inject constructor(
    private val engine: GemmaNarrationEngine,
) : ModelPackNarrationCoordinator {
    override fun onPackInstalled() {
        engine.reset()
    }

    override fun onPackRemoved() {
        engine.reset()
    }
}
