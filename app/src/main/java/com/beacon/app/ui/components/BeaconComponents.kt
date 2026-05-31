package com.beacon.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.theme.BeaconDimens

/**
 * Standard dark bento screen scaffold: near-black background, generous padding,
 * scrollable. Preferred layout for all Beacon screens.
 */
@Composable
fun BeaconScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = BeaconDimens.screenHorizontalPadding,
                    vertical = BeaconDimens.screenVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(BeaconDimens.sectionSpacing),
        ) {
            content()
        }
    }
}

/** A large bold title + optional subtitle (page heading, like the reference). */
@Composable
fun BeaconHeading(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Circular icon affordance (arrow / menu), as seen on the reference cards. */
@Composable
fun ArrowCircle(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.ArrowOutward,
    background: Color = Color.Black,
    tint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(BeaconDimens.arrowCircle)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

/**
 * Bento card: bold colored tile with a title, optional big value, and an
 * arrow-circle affordance. The whole card is one accessible tap target.
 */
@Composable
fun BentoCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    minHeight: androidx.compose.ui.unit.Dp = BeaconDimens.bentoLargeMinHeight,
    value: String? = null,
    leadingIcon: ImageVector? = null,
    showArrow: Boolean = true,
    enabled: Boolean = true,
    contentDescription: String = title,
) {
    val arrowBg = if (contentColor == Color.White || contentColor == MaterialTheme.colorScheme.onSurface) {
        contentColor
    } else {
        Color.Black
    }
    val arrowTint = if (arrowBg == Color.Black) Color.White else containerColor
    val clickAction = onClick

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(
                if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(BeaconDimens.cornerLarge),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Button
                if (enabled) onClick(action = { clickAction(); true }) else disabled()
            }
            .padding(BeaconDimens.cardPadding),
    ) {
        if (showArrow) {
            ArrowCircle(
                modifier = Modifier.align(Alignment.TopEnd),
                background = arrowBg,
                tint = arrowTint,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
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
                text = title,
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

/**
 * Large primary action button (used on flow screens like Welcome/Permissions).
 * Tall touch target, rounded, optional icon.
 */
@Composable
fun BigActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentDescription: String = label,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val clickAction = onClick
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BeaconDimens.primaryButtonMinHeight)
            .background(
                if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(BeaconDimens.cornerLarge),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Button
                if (enabled) onClick(action = { clickAction(); true }) else disabled()
            }
            .padding(horizontal = BeaconDimens.cardPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Secondary, lower-emphasis action: outlined pill on dark. */
@Composable
fun SecondaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = label,
) {
    val clickAction = onClick
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BeaconDimens.secondaryButtonMinHeight)
            .background(Color.Transparent, RoundedCornerShape(BeaconDimens.cornerLarge))
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Button
                if (enabled) onClick(action = { clickAction(); true }) else disabled()
            }
            .padding(horizontal = BeaconDimens.cardPadding),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color.Transparent,
            border = BorderStroke(BeaconDimens.cardBorderWidth, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(BeaconDimens.cornerLarge),
            modifier = Modifier.fillMaxWidth().heightIn(min = BeaconDimens.secondaryButtonMinHeight),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Grouped status/info in a dark bordered card (kept for status screens). */
@Composable
fun BeaconStatusCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BeaconDimens.cornerLarge),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(BeaconDimens.cardBorderWidth, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BeaconDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = if (emphasize) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun RowWithSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}
