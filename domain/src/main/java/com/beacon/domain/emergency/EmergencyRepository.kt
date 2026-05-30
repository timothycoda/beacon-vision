package com.beacon.domain.emergency

import com.beacon.core.result.OperationResult
import kotlinx.coroutines.flow.Flow

interface EmergencyRepository {
    val trustedContact: Flow<TrustedContact?>

    suspend fun saveTrustedContact(contact: TrustedContact)

    suspend fun clearTrustedContact()

    /**
     * Builds an emergency SMS draft: optional scene from glasses, last-known location,
     * trusted contact from storage.
     */
    suspend fun prepareAlert(includeSceneFromGlasses: Boolean): OperationResult<EmergencyAlertDraft>
}
