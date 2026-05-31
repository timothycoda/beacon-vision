package com.beacon.app.di

import com.beacon.app.modelpack.AndroidModelPackDownloadLauncher
import com.beacon.app.modelpack.GemmaModelPackNarrationCoordinator
import com.beacon.app.vision.GemmaSceneNarrationEnhancer
import com.beacon.data.modelpack.ModelPackDownloadLauncher
import com.beacon.data.modelpack.ModelPackNarrationCoordinator
import com.beacon.data.vision.SceneNarrationEnhancer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindModelPackDownloadLauncher(
        impl: AndroidModelPackDownloadLauncher,
    ): ModelPackDownloadLauncher

    @Binds
    @Singleton
    abstract fun bindSceneNarrationEnhancer(
        impl: GemmaSceneNarrationEnhancer,
    ): SceneNarrationEnhancer

    @Binds
    @Singleton
    abstract fun bindModelPackNarrationCoordinator(
        impl: GemmaModelPackNarrationCoordinator,
    ): ModelPackNarrationCoordinator
}
