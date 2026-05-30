package com.beacon.domain.location

import com.beacon.core.result.OperationResult
import com.beacon.domain.emergency.EmergencyLocation

/** Last-known device location for emergency messages. */
interface LocationProvider {
    suspend fun getLastKnown(): OperationResult<EmergencyLocation>
}
