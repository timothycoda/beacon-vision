package com.beacon.app.ui.read

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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.VolumeUp
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
import com.beacon.app.ui.accessibility.ScreenVoiceIntro
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.domain.accessibility.AccessibilityScreenIds
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.app.ui.theme.BeaconWhiteCard
import com.beacon.app.ui.theme.BeaconWhiteCardText

@Composable
fun ReadThisScreen(
    onBack: () -> Unit,
    autoStart: Boolean = false,
    viewModel: ReadThisViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (autoStart) viewModel.readText()
    }

    ScreenVoiceIntro(AccessibilityScreenIds.READ_THIS)

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "Read this",
            subtitle = "Capture text and Beacon will read it aloud.",
        )

        state.imageBytes?.let { bytes ->
            CapturedPreview(bytes)
        }

        StatusBlock(state.status)

        BentoCard(
            title = if (state.isBusy) "Working…" else "Read text",
            onClick = viewModel::readText,
            containerColor = BeaconWhiteCard,
            contentColor = BeaconWhiteCardText,
            leadingIcon = Icons.Filled.MenuBook,
            minHeight = BeaconDimens.bentoLargeMinHeight,
            showArrow = !state.isBusy,
            enabled = !state.isBusy,
            contentDescription = "Read text. Capture a photo and hear the text read aloud.",
        )

        val result = state.status as? ReadStatus.Result
        if (result != null && result.fullText.isNotBlank()) {
            BentoCard(
                title = "Read again",
                onClick = viewModel::repeatAloud,
                containerColor = BeaconCardDark,
                contentColor = BeaconOnDark,
                leadingIcon = Icons.Filled.VolumeUp,
                minHeight = BeaconDimens.bentoWideMinHeight,
                contentDescription = "Read the text aloud again.",
            )
        }

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
private fun StatusBlock(status: ReadStatus) {
    val (text, isError) = when (status) {
        ReadStatus.Idle -> "Tap Read text when you're ready." to false
        ReadStatus.Capturing -> "Capturing a photo…" to false
        ReadStatus.Reading -> "Reading the text…" to false
        is ReadStatus.Result ->
            if (status.fullText.isBlank()) status.spokenText to false
            else status.fullText to false
        is ReadStatus.Error -> status.message to true
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BeaconDimens.cornerLarge),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(BeaconDimens.cardPadding)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (status is ReadStatus.Capturing || status is ReadStatus.Reading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = text,
                    style = if (status is ReadStatus.Result && status.fullText.isNotBlank()) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}
