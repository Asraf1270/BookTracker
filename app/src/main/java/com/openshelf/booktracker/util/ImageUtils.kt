package com.openshelf.booktracker.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageUtils {

    /**
     * Copies the picked content:// URI into app-private files/covers/.
     * Returns the absolute path (as String) to store in BookEntity.coverImageUri.
     * Safe to call from IO dispatcher.
     */
    fun copyToInternalStorage(context: Context, uri: Uri): String? {
        return runCatching {
            val dir = File(context.filesDir, "covers").apply { mkdirs() }
            val dest = File(dir, "cover_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest.absolutePath
        }.getOrNull()
    }

    /** Deletes a previously saved cover file. Safe to call even if path is null. */
    fun deleteFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    fun exists(path: String?): Boolean =
        !path.isNullOrBlank() && File(path).exists()
}