package com.beacon.domain.modelpack

import com.beacon.domain.guidance.GuidanceLanguage

/** What the home screen shows for active offline models. */
data class ActiveModelPacksSummary(
    val intelligenceName: String,
    val voiceName: String?,
    val guidanceLanguage: GuidanceLanguage,
)
