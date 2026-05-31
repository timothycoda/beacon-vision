package com.beacon.app.modelpack

import android.content.Context
import androidx.core.content.ContextCompat
import com.beacon.data.modelpack.ModelPackDownloadLauncher
import com.beacon.domain.modelpack.ModelPackId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidModelPackDownloadLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : ModelPackDownloadLauncher {

    override fun startForegroundDownload(id: ModelPackId) {
        ContextCompat.startForegroundService(
            context,
            ModelPackDownloadService.intent(context, id),
        )
    }

    override fun stopForegroundDownload() {
        context.stopService(ModelPackDownloadService.intent(context, null))
    }
}
