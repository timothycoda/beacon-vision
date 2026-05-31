package com.beacon.domain.helper

import kotlinx.coroutines.flow.Flow

interface TrustedHelperRepository {
    val helpers: Flow<List<TrustedHelper>>

    suspend fun add(helper: TrustedHelper)
    suspend fun update(helper: TrustedHelper)
    suspend fun delete(id: String)
    suspend fun getPrimaryHelper(): TrustedHelper?
    suspend fun getPrimaryEmergencyContact(): TrustedHelper?
    suspend fun listWhatsAppEmergencyContacts(): List<TrustedHelper>
}
