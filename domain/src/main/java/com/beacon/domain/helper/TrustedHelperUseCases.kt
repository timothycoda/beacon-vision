package com.beacon.domain.helper

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTrustedHelpersUseCase @Inject constructor(
    private val repository: TrustedHelperRepository,
) {
    operator fun invoke(): Flow<List<TrustedHelper>> = repository.helpers
}

class AddTrustedHelperUseCase @Inject constructor(
    private val repository: TrustedHelperRepository,
) {
    suspend operator fun invoke(helper: TrustedHelper) = repository.add(helper)
}

class UpdateTrustedHelperUseCase @Inject constructor(
    private val repository: TrustedHelperRepository,
) {
    suspend operator fun invoke(helper: TrustedHelper) = repository.update(helper)
}

class DeleteTrustedHelperUseCase @Inject constructor(
    private val repository: TrustedHelperRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}

class GetPrimaryTrustedHelperUseCase @Inject constructor(
    private val repository: TrustedHelperRepository,
) {
    suspend operator fun invoke(): TrustedHelper? = repository.getPrimaryHelper()
}

class GetPrimaryEmergencyContactUseCase @Inject constructor(
    private val repository: TrustedHelperRepository,
) {
    suspend operator fun invoke(): TrustedHelper? = repository.getPrimaryEmergencyContact()
}

class ListWhatsAppEmergencyContactsUseCase @Inject constructor(
    private val repository: TrustedHelperRepository,
) {
    suspend operator fun invoke(): List<TrustedHelper> = repository.listWhatsAppEmergencyContacts()
}
