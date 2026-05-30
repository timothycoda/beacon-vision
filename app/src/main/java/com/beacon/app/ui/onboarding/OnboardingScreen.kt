package com.beacon.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.components.BigActionButton
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.app.ui.theme.BeaconBackground
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconLimeText
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.app.ui.theme.BeaconOnDarkMuted
import com.beacon.app.ui.theme.BeaconOutlineDark

private data class OnboardingPage(
    val title: String,
    val body: String,
    val illustration: @Composable () -> Unit,
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val pages = listOf(
        OnboardingPage(
            title = "What is ahead?",
            body = "Point your glasses forward. Beacon describes the scene in a short, clear voice cue.",
            illustration = { AheadIllustration() },
        ),
        OnboardingPage(
            title = "Read this",
            body = "Capture signs, labels, or documents. Beacon reads the text aloud—fully on your phone.",
            illustration = { ReadIllustration() },
        ),
        OnboardingPage(
            title = "Hands-free",
            body = "Long-press your glasses, say a command, and Beacon responds—even with the screen locked.",
            illustration = { HandsFreeIllustration() },
        ),
    )

    var pageIndex by remember { mutableIntStateOf(0) }
    val isLast = pageIndex == pages.lastIndex
    val content = pages[pageIndex]

    Surface(modifier = Modifier.fillMaxSize(), color = BeaconBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = BeaconDimens.screenHorizontalPadding,
                    vertical = BeaconDimens.screenVerticalPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Beacon",
                style = MaterialTheme.typography.titleLarge,
                color = BeaconOnDarkMuted,
            )
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                content.illustration()
                Spacer(Modifier.height(32.dp))
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = BeaconOnDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = content.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BeaconOnDarkMuted,
                    textAlign = TextAlign.Center,
                )
            }

            PageDots(count = pages.size, selected = pageIndex)
            Spacer(Modifier.height(24.dp))

            BigActionButton(
                label = if (isLast) "Continue" else "Next",
                onClick = {
                    if (isLast) {
                        onFinished()
                    } else {
                        pageIndex++
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = BeaconLime,
                contentColor = BeaconLimeText,
                contentDescription = if (isLast) "Continue to permissions" else "Next page",
            )
            if (!isLast) {
                Spacer(Modifier.height(12.dp))
                SecondaryActionButton(
                    label = "Skip",
                    onClick = onFinished,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = "Skip introduction",
                )
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .size(if (active) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (active) BeaconLime else BeaconOutlineDark),
            )
        }
    }
}
