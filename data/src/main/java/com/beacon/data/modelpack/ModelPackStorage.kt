package com.beacon.data.modelpack

import android.content.Context
import com.beacon.domain.modelpack.ModelPackId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelPackStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val root: File
        get() = File(context.filesDir, "model_packs").also { it.mkdirs() }

    fun packDir(id: ModelPackId): File = File(root, id.storageKey).also { it.mkdirs() }

    fun packFile(id: ModelPackId, fileName: String): File = File(packDir(id), fileName)

    fun isInstalled(id: ModelPackId, fileName: String): Boolean {
        val file = packFile(id, fileName)
        return file.isFile && file.length() > 0L
    }

    fun deletePack(id: ModelPackId) {
        packDir(id).deleteRecursively()
    }

    fun partialFile(id: ModelPackId, fileName: String): File =
        File(packDir(id), "$fileName.part")
}
