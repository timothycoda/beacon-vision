package com.beacon.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.R
import com.beacon.app.ui.components.BigActionButton
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.bentoWhiteCardColor
import com.beacon.app.ui.theme.bentoWhiteCardOnColor
import com.beacon.domain.device.GuidanceInputMode

private data class OnboardingPage(
    val title: String,
    val body: String,
    val illustration: @Composable () -> Unit,
)

@Composable
fun OnboardingScreen(
    onFinished: (GuidanceInputMode) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMode = state.selectedMode

    // Everyone sees the full product tour; device pick only sets the default path after setup.
    val featurePages = listOf(
        OnboardingPage(
            title = stringResource(R.string.onboarding_glasses_overview_title),
            body = stringResource(R.string.onboarding_glasses_overview_body),
            illustration = { GlassesOverviewIllustration() },
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_phone_overview_title),
            body = stringResource(R.string.onboarding_phone_overview_body),
            illustration = { PhoneCameraIllustration() },
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_hausa_title),
            body = stringResource(R.string.onboarding_hausa_body),
            illustration = { HausaIllustration() },
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_helpers_title),
            body = stringResource(R.string.onboarding_helpers_body),
            illustration = { TrustedHelpersIllustration() },
        ),
    )

    var stepIndex by remember { mutableIntStateOf(0) }
    val totalSteps = 1 + featurePages.size
    val isDeviceStep = stepIndex == 0
    val featureIndex = stepIndex - 1
    val isLast = stepIndex == totalSteps - 1
    val canAdvanceFromDevice = selectedMode != null

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = BeaconDimens.screenHorizontalPadding,
                    vertical = BeaconDimens.screenVerticalPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (isDeviceStep) {
                    DeviceChoiceStep(
                        selected = selectedMode,
                        onSelect = viewModel::selectMode,
                    )
                } else {
                    val page = featurePages[featureIndex]
                    page.illustration()
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (!isDeviceStep) {
                PageDots(count = featurePages.size, selected = featureIndex)
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(24.dp))
            }

            BigActionButton(
                label = when {
                    isDeviceStep -> stringResource(R.string.onboarding_choose_device_continue)
                    isLast -> stringResource(R.string.onboarding_finish)
                    else -> stringResource(R.string.onboarding_next)
                },
                onClick = {
                    when {
                        isDeviceStep && canAdvanceFromDevice -> stepIndex = 1
                        isLast && selectedMode != null ->
                            viewModel.persistModeAndFinish(onFinished)
                        !isLast -> stepIndex++
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDeviceStep || canAdvanceFromDevice,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentDescription = when {
                    isDeviceStep -> stringResource(R.string.onboarding_choose_device_continue_cd)
                    isLast -> stringResource(R.string.onboarding_finish_cd)
                    else -> stringResource(R.string.onboarding_next_cd)
                },
            )
            if (!isLast) {
                Spacer(Modifier.height(12.dp))
                SecondaryActionButton(
                    label = stringResource(R.string.onboarding_skip),
                    onClick = {
                        val mode = selectedMode ?: GuidanceInputMode.Glasses
                        viewModel.selectMode(mode)
                        viewModel.persistModeAndFinish(onFinished)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = stringResource(R.string.onboarding_skip_cd),
                )
            }
        }
    }
}

@Composable
private fun DeviceChoiceStep(
    selected: GuidanceInputMode?,
    onSelect: (GuidanceInputMode) -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_device_title),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_device_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    DeviceChoiceCard(
        title = stringResource(R.string.onboarding_device_glasses_title),
        description = stringResource(R.string.onboarding_device_glasses_body),
        icon = Icons.Filled.Visibility,
        selected = selected == GuidanceInputMode.Glasses,
        onClick = { onSelect(GuidanceInputMode.Glasses) },
    )
    Spacer(Modifier.height(12.dp))
    DeviceChoiceCard(
        title = stringResource(R.string.onboarding_device_phone_title),
        description = stringResource(R.string.onboarding_device_phone_body),
        icon = Icons.Filled.PhotoCamera,
        selected = selected == GuidanceInputMode.PhoneCamera,
        onClick = { onSelect(GuidanceInputMode.PhoneCamera) },
    )
}

@Composable
private fun DeviceChoiceCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val container = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BeaconDimens.cornerLarge))
            .background(container)
            .border(2.dp, borderColor, RoundedCornerShape(BeaconDimens.cornerLarge))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title. $description"
                this.selected = selected
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else bentoWhiteCardColor(),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else bentoWhiteCardOnColor(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    ),
            )
        }
    }
}
