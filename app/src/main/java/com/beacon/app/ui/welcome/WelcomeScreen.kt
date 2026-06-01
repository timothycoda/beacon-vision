package com.beacon.app.ui.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beacon.app.R
import com.beacon.app.ui.components.BigActionButton
import com.beacon.app.ui.components.BrandWelcomeLogo
import com.beacon.app.ui.components.rememberBrandDrawableId
import com.beacon.app.ui.theme.BeaconDimens

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val safety = stringResource(R.string.welcome_safety)
    val safetyCd = stringResource(R.string.welcome_safety_cd)
    val hasLogo = rememberBrandDrawableId("brand_welcome_logo") != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = BeaconDimens.screenHorizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (hasLogo) {
                    BrandWelcomeLogo(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.padding(top = 48.dp))
                BigActionButton(
                    label = "Get started",
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = stringResource(R.string.welcome_get_started_cd),
                )
            }

            Text(
                text = safety,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        horizontal = BeaconDimens.screenHorizontalPadding,
                        vertical = 28.dp,
                    )
                    .semantics { contentDescription = safetyCd },
            )
        }
    }
}
