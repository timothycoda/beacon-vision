package com.beacon.app.ui.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.components.BigActionButton
import com.beacon.app.ui.theme.BeaconBackground
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconLimeText
import com.beacon.app.ui.theme.BeaconOnDarkMuted

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BeaconBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = BeaconDimens.screenHorizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Beacon",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.padding(top = 48.dp))
                BigActionButton(
                    label = "Get started",
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = BeaconLime,
                    contentColor = BeaconLimeText,
                    contentDescription = "Get started. Learn what Beacon can do.",
                )
            }

            Text(
                text = "Beacon is an assistive companion. It can be wrong. Always use your cane, " +
                    "guide dog, caregiver support, and your own judgment.",
                style = MaterialTheme.typography.bodySmall,
                color = BeaconOnDarkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        horizontal = BeaconDimens.screenHorizontalPadding,
                        vertical = 28.dp,
                    )
                    .semantics {
                        contentDescription =
                            "Safety note: Beacon is an assistive companion and may be wrong."
                    },
            )
        }
    }
}
