package com.beacon.app

import android.app.Application
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.data.safety.GlassesHealthMonitor
import com.beacon.domain.modelpack.ReconcileModelPackDownloadsUseCase
import com.beacon.glasses.init.GlassesSdkInitializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class BeaconApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface InitializerEntryPoint {
        fun glassesSdkInitializer(): GlassesSdkInitializer
        fun glassesHealthMonitor(): GlassesHealthMonitor
        fun reconcileModelPackDownloads(): ReconcileModelPackDownloadsUseCase
        fun dispatchers(): DispatcherProvider
    }

    override fun onCreate() {
        super.onCreate()
        // The vendor BLE stack must be initialised once, before any glasses
        // operation. The initializer only sets up singletons and registers a
        // receiver; no blocking I/O on the main thread.
        val entryPoint = EntryPointAccessors.fromApplication(this, InitializerEntryPoint::class.java)
        entryPoint.glassesSdkInitializer().initialize()
        // Start connection/battery monitoring for spoken safety alerts.
        entryPoint.glassesHealthMonitor()
        val appScope = CoroutineScope(SupervisorJob() + entryPoint.dispatchers().io)
        appScope.launch {
            entryPoint.reconcileModelPackDownloads().invoke()
        }
    }
}
