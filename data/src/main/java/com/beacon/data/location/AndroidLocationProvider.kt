package com.beacon.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.permission.BeaconPermissions
import com.beacon.core.result.OperationResult
import com.beacon.domain.emergency.EmergencyLocation
import com.beacon.domain.location.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : LocationProvider {

    override suspend fun getLastKnown(): OperationResult<EmergencyLocation> =
        withContext(dispatchers.io) {
            if (!BeaconPermissions.hasEmergencyLocationPermission(context)) {
                return@withContext OperationResult.Failure("Location permission not granted.")
            }

            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext OperationResult.Failure("Location is not available on this device")

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
            val location = readLastKnown(manager, providers)

            if (location == null) {
                OperationResult.Failure("No recent location found. Turn on location and try again.")
            } else {
                OperationResult.Success(
                    EmergencyLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                    ),
                )
            }
        }

    @SuppressLint("MissingPermission")
    private fun readLastKnown(
        manager: LocationManager,
        providers: List<String>,
    ): android.location.Location? =
        providers
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
}
