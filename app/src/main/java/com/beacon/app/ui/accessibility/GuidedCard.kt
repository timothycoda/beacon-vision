package com.beacon.app.ui.accessibility

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.components.ArrowCircle
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.domain.accessibility.AccessibilityPhraseProvider
import com.beacon.domain.accessibility.SpeakableAction
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import com.beacon.app.accessibility.AccessibilityEntryPoint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size

/**
 * Accessible bento card with TalkBack semantics, optional Voice Guide focus/help speech,
 * and a "More help" custom accessibility action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuidedBentoCard(
    action: SpeakableAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    minHeight: Dp = BeaconDimens.bentoLargeMinHeight,
    value: String? = null,
    leadingIcon: ImageVector? = null,
    showArrow: Boolean = true,
    enabled: Boolean = true,
    phraseProvider: AccessibilityPhraseProvider = rememberPhraseProvider(),
) {
    val voiceGuide = LocalAccessibilityVoiceGuide.current
    val spokenText = action.spokenDescription
        ?: action.phraseKey?.let { phraseProvider.phrase(it) }
        ?: action.label
    val semanticsDescription = action.resolveContentDescription(spokenText)
    val helpLabel = action.phraseKey?.let { phraseProvider.phrase(it) } ?: spokenText

    val arrowBg = if (contentColor == Color.White || contentColor == MaterialTheme.colorScheme.onSurface) {
        contentColor
    } else {
        Color.Black
    }
    val arrowTint = if (arrowBg == Color.Black) Color.White else containerColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(
                if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(BeaconDimens.cornerLarge),
            )
            .combinedClickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    voiceGuide?.speakFocusedAction(action)
                    onClick()
                },
                onLongClick = {
                    voiceGuide?.speakActionHelp(action)
                },
            )
            .clearAndSetSemantics {
                contentDescription = semanticsDescription
                role = Role.Button
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = "More help",
                        action = {
                            voiceGuide?.speakActionHelp(action)
                            true
                        },
                    ),
                )
                if (enabled) {
                    onClick(label = "Activate", action = { onClick(); true })
                } else {
                    disabled()
                }
            }
            .padding(BeaconDimens.cardPadding),
    ) {
        if (showArrow) {
            ArrowCircle(
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd),
                background = arrowBg,
                tint = arrowTint,
            )
        }
        Column(
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(34.dp),
                )
            }
            Text(
                text = action.label,
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor,
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun rememberPhraseProvider(): AccessibilityPhraseProvider {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AccessibilityEntryPoint::class.java,
        ).accessibilityPhraseProvider()
    }
}
