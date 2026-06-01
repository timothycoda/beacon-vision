package com.beacon.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beacon.app.R

@Composable
fun rememberBrandDrawableId(baseName: String): Int? {
    val context = LocalContext.current
    return remember(baseName) {
        context.resources.getIdentifier(baseName, "drawable", context.packageName).takeIf { it != 0 }
    }
}

@Composable
fun BrandWelcomeLogo(
    modifier: Modifier = Modifier,
) {
    val logoId = rememberBrandDrawableId("brand_welcome_logo")
    val appName = stringResource(R.string.app_name)
    if (logoId != null) {
        Image(
            painter = painterResource(logoId),
            contentDescription = appName,
            modifier = modifier
                .heightIn(max = 160.dp)
                .semantics { contentDescription = appName },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun BrandWelcomeWordmark(
    modifier: Modifier = Modifier,
) {
    val wordmarkId = rememberBrandDrawableId("brand_welcome_wordmark")
    val appName = stringResource(R.string.app_name)
    if (wordmarkId != null) {
        Image(
            painter = painterResource(wordmarkId),
            contentDescription = appName,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 72.dp)
                .semantics { contentDescription = appName },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun BrandHeaderWordmark(
    modifier: Modifier = Modifier,
) {
    val wordmarkId = rememberBrandDrawableId("brand_header_wordmark")
    val appName = stringResource(R.string.app_name)
    if (wordmarkId != null) {
        Image(
            painter = painterResource(wordmarkId),
            contentDescription = appName,
            modifier = modifier
                .height(28.dp)
                .semantics { contentDescription = appName },
            contentScale = ContentScale.Fit,
        )
    }
}
