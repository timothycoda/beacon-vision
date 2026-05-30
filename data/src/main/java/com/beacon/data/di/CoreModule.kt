package com.beacon.data.di

import android.content.Context
import com.beacon.core.bluetooth.BluetoothStateMonitor
import com.beacon.core.bluetooth.BluetoothStateProvider
import com.beacon.core.concurrency.DefaultDispatcherProvider
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.haptics.Haptics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides cross-cutting :core utilities to the app-wide Hilt graph. Placed in
 * :data since :core has no DI plugin, and the SingletonComponent aggregates
 * modules from every module in the build.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideBluetoothStateProvider(@ApplicationContext context: Context): BluetoothStateProvider =
        BluetoothStateMonitor(context)

    @Provides
    @Singleton
    fun provideHaptics(@ApplicationContext context: Context): Haptics = Haptics(context)
}
