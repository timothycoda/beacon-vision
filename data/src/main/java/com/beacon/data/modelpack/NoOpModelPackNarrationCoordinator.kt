package com.beacon.data.modelpack

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpModelPackNarrationCoordinator @Inject constructor() : ModelPackNarrationCoordinator {
    override fun onPackInstalled() = Unit
    override fun onPackRemoved() = Unit
}
