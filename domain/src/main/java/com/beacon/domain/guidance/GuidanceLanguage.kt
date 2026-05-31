package com.beacon.domain.guidance

/** Language for spoken guidance and on-screen labels. */
enum class GuidanceLanguage(val storageKey: String) {
    English("en"),
    Hausa("ha"),
    ;

    companion object {
        fun fromStorageKey(key: String?): GuidanceLanguage =
            entries.firstOrNull { it.storageKey == key } ?: English
    }
}
