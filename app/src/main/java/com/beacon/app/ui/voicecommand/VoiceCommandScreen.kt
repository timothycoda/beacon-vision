package com.beacon.app.ui.voicecommand

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDanger
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.core.permission.BeaconPermissions
import com.beacon.domain.voice.VoiceCommand

@Composable
fun VoiceCommandScreen(
    onWhatIsAhead: () -> Unit,
    onReadThis: () -> Unit,
    onWalkingMode: () -> Unit,
    onEmergency: () -> Unit,
    onGlassesStatus: () -> Unit,
    onVoiceSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: VoiceCommandViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    fun listen() {
        if (BeaconPermissions.hasMicrophonePermission(context)) {
            viewModel.startListening()
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-start listening when the screen opens.
    LaunchedEffect(Unit) { listen() }

    // Route recognised commands to the matching feature.
    LaunchedEffect(viewModel) {
        viewModel.commands.collect { command ->
            when (command) {
                VoiceCommand.WhatIsAhead -> onWhatIsAhead()
                VoiceCommand.ReadText -> onReadThis()
                VoiceCommand.WalkingMode -> onWalkingMode()
                VoiceCommand.Emergency -> onEmergency()
                VoiceCommand.GlassesStatus -> onGlassesStatus()
                VoiceCommand.VoiceSettings -> onVoiceSettings()
                VoiceCommand.GoHome -> onBack()
                VoiceCommand.StopSpeaking -> Unit
                is VoiceCommand.Unknown -> Unit
            }
        }
    }

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "Voice command",
            subtitle = "Say what you need. For example: \u201Cwhat is ahead\u201D, \u201Cread this\u201D, \u201Cwalking mode\u201D, or \u201Cemergency\u201D.",
        )

        MicButton(
            listening = state.isListening,
            enabled = state.available,
            onTap = ::listen,
        )

        StatusText(state.status)

        BentoCard(
            title = if (state.isListening) "Listening…" else "Tap to speak",
            onClick = ::listen,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.Black,
            leadingIcon = Icons.Filled.Mic,
            minHeight = BeaconDimens.bentoWideMinHeight,
            showArrow = false,
            enabled = state.available && !state.isListening,
            contentDescription = "Tap to speak a command.",
        )

        BentoCard(
            title = "Back",
            onClick = {
                viewModel.cancelListening()
                onBack()
            },
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Go back to home.",
        )
    }
}

@Composable
private fun MicButton(
    listening: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "mic-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic-scale",
    )
    val background = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        listening -> MaterialTheme.colorScheme.primary
        else -> BeaconCardDark
    }
    val tint = if (listening) Color.Black else BeaconOnDark

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(if (listening) pulse else 1f)
                .background(background, CircleShape)
                .clickable(enabled = enabled, onClick = onTap)
                .semantics {
                    contentDescription = if (listening) "Listening. Speak now." else "Microphone. Tap to speak."
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

@Composable
private fun StatusText(status: VoiceStatus) {
    val (text, color) = when (status) {
        VoiceStatus.Idle -> "Tap the microphone, then speak." to MaterialTheme.colorScheme.onSurfaceVariant
        VoiceStatus.Preparing -> "Getting ready…" to MaterialTheme.colorScheme.onSurfaceVariant
        VoiceStatus.Listening -> "Listening… speak now." to MaterialTheme.colorScheme.onBackground
        is VoiceStatus.Hearing -> "\u201C${status.partial}\u201D" to MaterialTheme.colorScheme.onBackground
        is VoiceStatus.Recognized -> "Okay: ${status.label}" to MaterialTheme.colorScheme.primary
        is VoiceStatus.NotUnderstood -> "I heard \u201C${status.transcript}\u201D but didn't recognise a command." to MaterialTheme.colorScheme.onSurfaceVariant
        is VoiceStatus.Error -> status.message to BeaconDanger
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
