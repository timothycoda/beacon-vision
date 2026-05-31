package com.beacon.data.modelpack

import com.beacon.domain.modelpack.ModelPackId
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelPackPaths @Inject constructor(
    private val storage: ModelPackStorage,
) {
    fun installedNarrationModelFile(preferredId: ModelPackId?): File? {
        val order = buildList {
            if (preferredId != null) add(preferredId)
            add(ModelPackId.BALANCED)
            add(ModelPackId.GEMMA_4_LITE)
        }.distinct()
        for (id in order) {
            val entry = ModelPackCatalog.find(id) ?: continue
            val file = storage.packFile(id, entry.fileName)
            if (file.isFile && file.length() > 0L) return file
        }
        return null
    }

    fun isHausaVoiceInstalled(): Boolean {
        val entry = ModelPackCatalog.find(ModelPackId.HAUSA_VOICE) ?: return false
        return storage.isInstalled(ModelPackId.HAUSA_VOICE, entry.fileName)
    }

    fun modelFile(id: ModelPackId): File? {
        val entry = ModelPackCatalog.find(id) ?: return null
        val file = storage.packFile(id, entry.fileName)
        return file.takeIf { it.isFile && it.length() > 0L }
    }
}
