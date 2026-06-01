package com.beacon.app.ui.voice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconLimeText
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.app.ui.theme.BeaconWhiteCard
import com.beacon.app.ui.theme.BeaconWhiteCardText
import com.beacon.domain.speech.SpeechSettings

@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    viewModel: VoiceSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "Voice",
            subtitle = "Lock in the voice you like. Beacon saves your choice on this phone.",
        )

        state.statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BentoCard(
            title = "Keep voice I use now",
            onClick = viewModel::savePhonesCurrentVoice,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            minHeight = BeaconDimens.bentoLargeMinHeight,
            contentDescription = "Save your phone's current text-to-speech voice for Beacon.",
        )

        SectionTitle("Speech engine")
        SelectableRow(
            label = "System default",
            selected = state.selectedEngine == null,
            onClick = { viewModel.selectEngine(null) },
        )
        state.engines.forEach { engine ->
            SelectableRow(
                label = engine.label,
                selected = state.selectedEngine == engine.packageName,
                onClick = { viewModel.selectEngine(engine.packageName) },
            )
        }

        SectionTitle("Voice")
        SelectableRow(
            label = "Engine default",
            selected = state.selectedVoice == null,
            onClick = { viewModel.selectVoice(null) },
        )
        state.voices.take(12).forEach { voice ->
            SelectableRow(
                label = voice.label,
                selected = state.selectedVoice == voice.name,
                onClick = { viewModel.selectVoice(voice.name) },
            )
        }
        if (state.voices.size > 12) {
            Text(
                text = "${state.voices.size - 12} more voices available in system TTS settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle("Speed")
        Text(
            text = "Rate: ${"%.1f".format(state.speechRate)}×",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Slider(
            value = state.speechRate,
            onValueChange = viewModel::setSpeechRate,
            valueRange = SpeechSettings.MIN_SPEECH_RATE..SpeechSettings.MAX_SPEECH_RATE,
            modifier = Modifier.fillMaxWidth(),
        )

        BentoCard(
            title = "Test voice",
            onClick = viewModel::testVoice,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Play a short sample with the selected voice.",
        )

        BentoCard(
            title = "Reset to system default",
            onClick = viewModel::resetToSystemDefault,
            containerColor = BeaconWhiteCard,
            contentColor = BeaconWhiteCardText,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Reset voice settings to system default.",
        )

        BentoCard(
            title = "Back",
            onClick = onBack,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Go back.",
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(BeaconDimens.cornerMedium),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BeaconDimens.cardPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
            if (selected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
