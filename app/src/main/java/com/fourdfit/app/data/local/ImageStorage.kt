package com.fourdfit.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Copies a picked photo into private app storage, downscaled to [MAX_SIZE] px.
 * Re-encoding drops EXIF metadata (location, device) for privacy.
 * The photo stays on the device and is not uploaded.
 */
class ImageStorage(
    private val context: Context,
) {
    private val dir: File get() = File(context.filesDir, "profile").apply { mkdirs() }

    suspend fun saveProfilePhoto(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = decode(uri) ?: return@runCatching null
                val target = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(target).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out) }
                bitmap.recycle()
                target.absolutePath
            }.getOrNull()
        }

    /** Deletes every stored photo except [keepPath] (call after the profile is saved). */
    suspend fun cleanupExcept(keepPath: String?) =
        withContext(Dispatchers.IO) {
            dir.listFiles()?.filter { it.absolutePath != keepPath }?.forEach { it.delete() }
            Unit
        }

    suspend fun clear() =
        withContext(Dispatchers.IO) {
            dir.listFiles()?.forEach { it.delete() }
            Unit
        }

    private fun decode(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val largest = max(info.size.width, info.size.height)
                if (largest > MAX_SIZE) {
                    val scale = MAX_SIZE.toFloat() / largest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    }

    private companion object {
        const val MAX_SIZE = 512
    }
}
