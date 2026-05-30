package com.beacon.glasses.di

import com.beacon.glasses.BuildConfig
import com.beacon.glasses.FakeGlassesManager
import com.beacon.glasses.GlassesManager
import com.beacon.glasses.HeyCyanGlassesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GlassesModule {

    /**
     * Selects the glasses backend at runtime. Real hardware uses
     * [HeyCyanGlassesManager]; flipping `USE_FAKE_GLASSES` (e.g. for the
     * emulator) swaps in [FakeGlassesManager]. Providers avoid constructing the
     * unused implementation.
     */
    @Provides
    @Singleton
    fun provideGlassesManager(
        real: Provider<HeyCyanGlassesManager>,
        fake: Provider<FakeGlassesManager>,
    ): GlassesManager = if (BuildConfig.USE_FAKE_GLASSES) fake.get() else real.get()
}
