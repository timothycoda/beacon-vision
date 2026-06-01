package com.beacon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beacon.app.R

/**
 * App top bar: flavor wordmark when available, otherwise dot + app name.
 */
@Composable
fun BeaconTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val appName = stringResource(R.string.app_name)
    val barTitle = title ?: appName
    val headerWordmarkId = rememberBrandDrawableId("brand_header_wordmark")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (headerWordmarkId != null) {
                BrandHeaderWordmark()
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Text(
                    text = barTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .semantics { heading() },
                )
            }
        }
        if (trailingIcon != null && onTrailingClick != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.onBackground, CircleShape)
                    .clickable(onClick = onTrailingClick)
                    .clearAndSetSemantics {
                        contentDescription = trailingContentDescription ?: ""
                        role = Role.Button
                        onClick(action = { onTrailingClick(); true })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.background,
                )
            }
        }
    }
}

/** A pill chip, optionally "selected" (white fill) — mirrors the reference filter row. */
@Composable
fun BeaconChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val container = when {
        selected && accent != null -> accent
        selected -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when {
        selected && accent != null -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.background
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(container, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = content,
        )
    }
}
