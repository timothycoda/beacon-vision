package com.beacon.data.helper

import com.beacon.data.emergency.TrustedContactPreferences
import com.beacon.domain.helper.TrustedHelper
import com.beacon.domain.helper.TrustedHelperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustedHelperRepositoryImpl @Inject constructor(
    private val prefs: TrustedHelperPreferences,
    private val legacyContact: TrustedContactPreferences,
) : TrustedHelperRepository {

    override val helpers: Flow<List<TrustedHelper>> = prefs.helpers

    override suspend fun add(helper: TrustedHelper) {
        migrateLegacyIfNeeded()
        val list = normalizeList(prefs.readAll() + helper.copy(updatedAt = System.currentTimeMillis()))
        prefs.saveAll(applyPrimaryFlags(list, helper))
        syncLegacyPrimary(list)
    }

    override suspend fun update(helper: TrustedHelper) {
        migrateLegacyIfNeeded()
        val list = prefs.readAll().map {
            if (it.id == helper.id) helper.copy(updatedAt = System.currentTimeMillis()) else it
        }
        prefs.saveAll(applyPrimaryFlags(list, helper))
        syncLegacyPrimary(list)
    }

    override suspend fun delete(id: String) {
        migrateLegacyIfNeeded()
        val list = prefs.readAll().filterNot { it.id == id }
        prefs.saveAll(list)
        syncLegacyPrimary(list)
    }

    override suspend fun getPrimaryHelper(): TrustedHelper? {
        migrateLegacyIfNeeded()
        return prefs.readAll().firstOrNull { it.isPrimaryHelper && it.whatsappEnabled }
            ?: prefs.readAll().firstOrNull { it.whatsappEnabled }
    }

    override suspend fun getPrimaryEmergencyContact(): TrustedHelper? {
        migrateLegacyIfNeeded()
        return prefs.readAll().firstOrNull { it.isPrimaryEmergencyContact }
            ?: prefs.readAll().firstOrNull()
    }

    override suspend fun listWhatsAppEmergencyContacts(): List<TrustedHelper> {
        migrateLegacyIfNeeded()
        return prefs.readAll().filter { it.receiveEmergencyWhatsAppAlerts && it.whatsappEnabled }
    }

    private suspend fun migrateLegacyIfNeeded() {
        if (prefs.readAll().isNotEmpty()) return
        val legacy = legacyContact.contact.first() ?: return
        val migrated = TrustedHelper(
            displayName = legacy.name.ifBlank { "Trusted contact" },
            phoneNumber = legacy.phoneNumber,
            isPrimaryHelper = true,
            isPrimaryEmergencyContact = true,
        )
        prefs.saveAll(listOf(migrated))
    }

    private fun applyPrimaryFlags(list: List<TrustedHelper>, changed: TrustedHelper): List<TrustedHelper> {
        return list.map { item ->
            when {
                item.id == changed.id -> changed
                changed.isPrimaryHelper && item.isPrimaryHelper ->
                    item.copy(isPrimaryHelper = false)
                changed.isPrimaryEmergencyContact && item.isPrimaryEmergencyContact ->
                    item.copy(isPrimaryEmergencyContact = false)
                else -> item
            }
        }
    }

    private fun normalizeList(list: List<TrustedHelper>): List<TrustedHelper> {
        if (list.isEmpty()) return list
        var result = list
        if (result.none { it.isPrimaryHelper }) {
            result = result.mapIndexed { index, h ->
                if (index == 0) h.copy(isPrimaryHelper = true) else h
            }
        }
        if (result.none { it.isPrimaryEmergencyContact }) {
            result = result.mapIndexed { index, h ->
                if (index == 0) h.copy(isPrimaryEmergencyContact = true) else h
            }
        }
        return result
    }

    private suspend fun syncLegacyPrimary(list: List<TrustedHelper>) {
        val primary = list.firstOrNull { it.isPrimaryEmergencyContact } ?: return
        legacyContact.save(primary.toTrustedContact())
    }
}
