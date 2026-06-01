package com.beacon.app.ui.accessibility

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.components.RowWithSwitch
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.domain.accessibility.AccessibilityScreenIds
import com.beacon.domain.accessibility.VoiceGuideMode

@Composable
fun AccessibilitySettingsScreen(
    onBack: () -> Unit,
    viewModel: AccessibilitySettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val guide = LocalAccessibilityVoiceGuide.current ?: rememberAccessibilityVoiceGuide()

    ScreenVoiceIntro(AccessibilityScreenIds.ACCESSIBILITY_SETTINGS)

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "Accessibility",
            subtitle = "Voice Guide works alongside TalkBack. Choose when the app speaks navigation hints.",
        )

        RowWithSwitch(
            label = "Voice Guide",
            checked = settings.voiceGuideEnabled,
            onCheckedChange = viewModel::setVoiceGuideEnabled,
        )

        Text(
            text = "Voice Guide mode",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceGuideModeOption(
                title = "Always speak",
                subtitle = "Screen intros and control hints even when TalkBack is on.",
                selected = settings.voiceGuideMode == VoiceGuideMode.Always,
                onSelect = { viewModel.setVoiceGuideMode(VoiceGuideMode.Always) },
            )
            VoiceGuideModeOption(
                title = "Important only",
                subtitle = "Emergencies, errors, and warnings.",
                selected = settings.voiceGuideMode == VoiceGuideMode.ImportantOnly,
                onSelect = { viewModel.setVoiceGuideMode(VoiceGuideMode.ImportantOnly) },
            )
            VoiceGuideModeOption(
                title = "When TalkBack is off",
                subtitle = "Recommended. Avoids doubling TalkBack speech.",
                selected = settings.voiceGuideMode == VoiceGuideMode.WhenTalkBackOff,
                onSelect = { viewModel.setVoiceGuideMode(VoiceGuideMode.WhenTalkBackOff) },
            )
        }

        RowWithSwitch(
            label = "Speak focused controls",
            checked = settings.speakFocusedControls,
            onCheckedChange = viewModel::setSpeakFocusedControls,
        )
        RowWithSwitch(
            label = "Repeat screen intro",
            checked = settings.repeatScreenIntro,
            onCheckedChange = viewModel::setRepeatScreenIntro,
        )
        RowWithSwitch(
            label = "Haptic feedback",
            checked = settings.hapticFeedbackEnabled,
            onCheckedChange = viewModel::setHaptics,
        )

        Text(
            text = if (guide.isTalkBackActive()) {
                "TalkBack appears to be on. “When TalkBack is off” avoids duplicate speech."
            } else {
                "TalkBack does not appear to be on. Voice Guide can speak navigation hints."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        BentoCard(
            title = "Back",
            onClick = onBack,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Go back to settings.",
        )
    }
}

@Composable
private fun VoiceGuideModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    BentoCard(
        title = title,
        value = subtitle,
        onClick = onSelect,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        minHeight = 72.dp,
        showArrow = false,
        contentDescription = "$title. $subtitle",
    )
}
