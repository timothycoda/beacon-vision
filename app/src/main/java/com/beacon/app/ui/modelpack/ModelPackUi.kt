package com.beacon.app.ui.modelpack

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.beacon.app.BuildConfig
import com.beacon.app.R
import com.beacon.domain.modelpack.ModelPackId
import com.beacon.domain.modelpack.ModelPackStatus

@Composable
fun ModelPackStatus.brandDisplayName(): String {
    if (BuildConfig.BRAND != "elenii") return displayName
    val resId = when (id) {
        ModelPackId.HAUSA_VOICE -> R.string.model_pack_hausa_voice
        ModelPackId.BALANCED -> R.string.model_pack_vision_pro
        ModelPackId.GEMMA_4_LITE -> R.string.model_pack_vision_plus
        else -> null
    }
    return resId?.let { stringResource(it) } ?: displayName
}
