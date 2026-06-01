package com.beacon.app.ui.ahead

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
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

@Composable
fun WhatIsAheadScreen(
    onBack: () -> Unit,
    autoStart: Boolean = false,
    viewModel: WhatIsAheadViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (autoStart) viewModel.lookAhead()
    }

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "What is ahead?",
            subtitle = "Capture a photo and Beacon will describe it.",
        )

        state.imageBytes?.let { bytes ->
            CapturedPreview(bytes)
        }

        StatusBlock(state.status)

        BentoCard(
            title = if (state.isBusy) "Working…" else "Look ahead",
            onClick = viewModel::lookAhead,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            leadingIcon = Icons.Filled.Visibility,
            minHeight = BeaconDimens.bentoLargeMinHeight,
            showArrow = !state.isBusy,
            enabled = !state.isBusy,
            contentDescription = "Look ahead. Capture a photo and hear what is in front of you.",
        )

        BentoCard(
            title = "Back",
            onClick = onBack,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Go back to home.",
        )
    }
}

@Composable
private fun CapturedPreview(bytes: ByteArray) {
    val bitmap = remember(bytes) {
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
    } ?: return
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(BeaconDimens.cornerLarge)),
    )
}

@Composable
private fun StatusBlock(status: AheadStatus) {
    val (text, isError) = when (status) {
        AheadStatus.Idle -> "Tap Look ahead when you're ready." to false
        AheadStatus.Capturing -> "Capturing a photo…" to false
        AheadStatus.Analyzing -> "Looking at the scene…" to false
        is AheadStatus.Result -> status.summary to false
        is AheadStatus.Error -> status.message to true
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BeaconDimens.cornerLarge),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(BeaconDimens.cardPadding)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (status is AheadStatus.Capturing || status is AheadStatus.Analyzing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}
