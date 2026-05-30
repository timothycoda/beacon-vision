package com.beacon.data.emergency

import com.beacon.core.result.OperationResult
import com.beacon.domain.emergency.EmergencyAlertDraft
import com.beacon.domain.emergency.EmergencyMessageBuilder
import com.beacon.domain.emergency.EmergencyRepository
import com.beacon.domain.emergency.TrustedContact
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.location.LocationProvider
import com.beacon.domain.vision.usecase.CapturePhotoUseCase
import com.beacon.domain.vision.usecase.DescribeSceneUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyRepositoryImpl @Inject constructor(
    private val contactPreferences: TrustedContactPreferences,
    private val locationProvider: LocationProvider,
    private val glassesRepository: GlassesRepository,
    private val capturePhoto: CapturePhotoUseCase,
    private val describeScene: DescribeSceneUseCase,
) : EmergencyRepository {

    override val trustedContact: Flow<TrustedContact?> = contactPreferences.contact

    override suspend fun saveTrustedContact(contact: TrustedContact) {
        contactPreferences.save(contact)
    }

    override suspend fun clearTrustedContact() {
        contactPreferences.clear()
    }

    override suspend fun prepareAlert(includeSceneFromGlasses: Boolean): OperationResult<EmergencyAlertDraft> {
        val contact = contactPreferences.contact.first()
            ?: return OperationResult.Failure("Add a trusted contact first.")

        if (contact.phoneNumber.isBlank()) {
            return OperationResult.Failure("Trusted contact phone number is missing.")
        }

        val location = when (val loc = locationProvider.getLastKnown()) {
            is OperationResult.Success -> loc.value
            is OperationResult.Failure -> null
        }

        val sceneSummary = if (includeSceneFromGlasses && glassesRepository.isConnected()) {
            when (val capture = capturePhoto()) {
                is OperationResult.Success ->
                    when (val described = describeScene(capture.value)) {
                        is OperationResult.Success -> described.value.spokenSummary
                        is OperationResult.Failure -> null
                    }
                is OperationResult.Failure -> null
            }
        } else {
            null
        }

        val message = EmergencyMessageBuilder.build(
            contactName = contact.name.takeIf { it.isNotBlank() },
            location = location,
            sceneSummary = sceneSummary,
        )

        return OperationResult.Success(
            EmergencyAlertDraft(
                phoneNumber = contact.phoneNumber,
                message = message,
            ),
        )
    }
}
