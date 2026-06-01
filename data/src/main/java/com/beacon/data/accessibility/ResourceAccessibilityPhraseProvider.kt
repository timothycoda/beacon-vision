package com.beacon.data.accessibility

import android.content.Context
import com.beacon.domain.accessibility.AccessibilityPhraseKey
import com.beacon.domain.accessibility.AccessibilityPhraseProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceAccessibilityPhraseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AccessibilityPhraseProvider {

    override fun phrase(key: AccessibilityPhraseKey): String {
        val resId = context.resources.getIdentifier(
            key.stringResourceName,
            "string",
            context.packageName,
        )
        if (resId == 0) {
            return key.resSuffix.replace('_', ' ')
        }
        return context.getString(resId)
    }
}
