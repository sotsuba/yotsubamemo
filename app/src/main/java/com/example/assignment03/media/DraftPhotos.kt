package com.example.assignment03.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.UUID

/** Temporary, app-owned photos. Call copying/validation/cleanup on Dispatchers.IO. */
class DraftPhotos(private val context: Context) {
    private val directory get() = File(context.cacheDir, "draft_images")

    fun file(name: String): File {
        require(name.matches(Regex("[a-f0-9-]+\\.jpg"))) { "Invalid draft path" }
        return File(directory, name)
    }

    fun create(): String {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create draft directory" }
        val name = "${UUID.randomUUID()}.jpg"
        check(file(name).createNewFile()) { "Cannot create draft" }
        return name
    }

    fun cameraUri(name: String): Uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file(name)
    )

    fun localUri(name: String): Uri = Uri.fromFile(file(name))

    fun validate(name: String) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file(name).absolutePath, options)
        require(file(name).length() > 0 && options.outWidth > 0 && options.outHeight > 0 &&
            options.outMimeType?.startsWith("image/") == true) { "Cannot read photo" }
    }

    fun copy(uri: Uri): String {
        val name = create()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file(name).outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Cannot read selected photo")
            validate(name)
            return name
        } catch (error: Exception) {
            delete(name)
            throw error
        }
    }

    fun delete(name: String?) { name?.let { file(it).delete() } }

    fun cleanOld(protected: Set<String>) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory.listFiles()?.filter { it.isFile && it.name !in protected && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }
}
