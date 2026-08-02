package com.financetracker.evolva.data.receipt

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ReceiptStore {
    private fun dir(context: Context): File =
        File(context.filesDir, "receipts").apply { mkdirs() }

    /** Copy a picked image into app-private storage; returns absolute path. */
    fun copyFromUri(context: Context, uri: Uri): String? {
        return try {
            val ext = context.contentResolver.getType(uri)
                ?.substringAfterLast('/')
                ?.takeIf { it in listOf("jpeg", "jpg", "png", "webp") }
                ?: "jpg"
            val dest = File(dir(context), "${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteIfOwned(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        val receipts = dir(context)
        if (file.exists() && file.canonicalPath.startsWith(receipts.canonicalPath)) {
            file.delete()
        }
    }
}
