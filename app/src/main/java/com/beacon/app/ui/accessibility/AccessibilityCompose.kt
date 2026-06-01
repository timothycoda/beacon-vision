package com.beacon.app.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.beacon.app.accessibility.AccessibilityEntryPoint
import com.beacon.domain.accessibility.AccessibilityVoiceGuide
import dagger.hilt.android.EntryPointAccessors

val LocalAccessibilityVoiceGuide = compositionLocalOf<AccessibilityVoiceGuide?> { null }

@Composable
fun rememberAccessibilityVoiceGuide(): AccessibilityVoiceGuide {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AccessibilityEntryPoint::class.java,
        ).accessibilityVoiceGuide()
    }
}

@Composable
fun ProvideAccessibilityVoiceGuide(
    guide: AccessibilityVoiceGuide = rememberAccessibilityVoiceGuide(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAccessibilityVoiceGuide provides guide) {
        content()
    }
}

/** Speaks a screen introduction when Voice Guide is enabled. */
@Composable
fun ScreenVoiceIntro(screenId: String) {
    val guide = LocalAccessibilityVoiceGuide.current ?: return
    LaunchedEffect(screenId) {
        guide.onScreenOpened(screenId)
    }
}
