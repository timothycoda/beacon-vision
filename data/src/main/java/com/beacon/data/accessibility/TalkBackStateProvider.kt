package com.beacon.data.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TalkBackStateProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isTalkBackTouchExplorationEnabled(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        return manager.isEnabled && manager.isTouchExplorationEnabled
    }
}
