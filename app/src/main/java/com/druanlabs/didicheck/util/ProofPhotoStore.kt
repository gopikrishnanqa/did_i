package com.druanlabs.didicheck.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Stores routine photo proofs in app-private files so gallery deletes
 * never crash the app — missing files are treated as unavailable.
 */
object ProofPhotoStore {
    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val CACHE_DIR = "camera"
    private const val PROOFS_DIR = "proofs"

    fun authority(context: Context): String = "${context.packageName}$AUTHORITY_SUFFIX"

    fun createCaptureUri(context: Context): Pair<File, Uri>? {
        return try {
            val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            if (!file.exists()) file.createNewFile()
            val uri = FileProvider.getUriForFile(context, authority(context), file)
            file to uri
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Copies a captured/picked image into durable private storage.
     * Returns absolute path, or null if anything fails.
     */
    fun persistProof(context: Context, source: File, proofId: String): String? {
        return try {
            if (!source.exists() || source.length() <= 0L) return null
            val dir = File(context.filesDir, PROOFS_DIR).apply { mkdirs() }
            val dest = File(dir, "$proofId.jpg")
            source.inputStream().use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            runCatching { source.delete() }
            if (dest.exists() && dest.length() > 0L) dest.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    fun persistProofFromUri(context: Context, uri: Uri, proofId: String): String? {
        return try {
            val dir = File(context.filesDir, PROOFS_DIR).apply { mkdirs() }
            val dest = File(dir, "$proofId.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            } ?: return null
            if (dest.exists() && dest.length() > 0L) dest.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    fun exists(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            val file = File(path)
            file.exists() && file.isFile && file.length() > 0L && file.canRead()
        } catch (_: Exception) {
            false
        }
    }

    fun deleteQuietly(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).delete()
        } catch (_: Exception) {
            // ignore
        }
    }

    fun decodeSafely(path: String?, maxSide: Int = 1080): Bitmap? {
        if (!exists(path)) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            while (largest / sample > maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }
}
