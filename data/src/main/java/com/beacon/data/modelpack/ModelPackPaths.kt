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
        val dir = storage.packDir(ModelPackId.HAUSA_VOICE)
        val model = java.io.File(dir, HAUSA_MODEL_FILE)
        val tokens = java.io.File(dir, HAUSA_TOKENS_FILE)
        return model.isFile && model.length() > 0L && tokens.isFile && tokens.length() > 0L
    }

    fun hausaVoiceModelDir(): java.io.File = storage.packDir(ModelPackId.HAUSA_VOICE)

    private companion object {
        const val HAUSA_MODEL_FILE = "model.onnx"
        const val HAUSA_TOKENS_FILE = "tokens.txt"
    }

    fun modelFile(id: ModelPackId): File? {
        val entry = ModelPackCatalog.find(id) ?: return null
        val file = storage.packFile(id, entry.fileName)
        return file.takeIf { it.isFile && it.length() > 0L }
    }
}
