package com.example.assignment03.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

/** Blocking file operations; the repository calls these only on its IO dispatcher. */
internal interface PhotoStore {
    fun importPhoto(uri: Uri): String
    fun uri(path: String): String?
    fun delete(path: String)
    fun cleanOrphans(referenced: Set<String>)
}

internal class InternalPhotoStore(context: Context) : PhotoStore {
    private val resolver = context.applicationContext.contentResolver
    private val root = File(context.applicationContext.filesDir, "moment_photos")

    override fun importPhoto(uri: Uri): String {
        require(uri.scheme in setOf("content", "file", "android.resource")) { "Unsupported photo URI" }
        check(root.isDirectory || root.mkdirs()) { "Cannot create photo directory" }
        val name = UUID.randomUUID().toString()
        val temporary = File(root, "$name.tmp")
        val destination = File(root, name)
        try {
            resolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Cannot open photo")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.absolutePath, bounds)
            require(temporary.length() > 0 && bounds.outWidth > 0 && bounds.outHeight > 0 &&
                bounds.outMimeType?.startsWith("image/") == true) { "Not a readable image" }
            check(temporary.renameTo(destination)) { "Cannot finish photo import" }
            return "moment_photos/$name"
        } finally {
            temporary.delete()
        }
    }

    private fun file(path: String): File {
        require(path.startsWith("moment_photos/")) { "Invalid managed photo path" }
        val name = path.removePrefix("moment_photos/")
        require(name.isNotBlank() && name != "." && name != ".." && '/' !in name && '\\' !in name)
        return File(root, name)
    }

    override fun uri(path: String): String? = file(path).takeIf { it.isFile }?.let {
        Uri.fromFile(it).toString()
    }

    // A failed deletion is retried by cleanup on the next repository initialization.
    override fun delete(path: String) { file(path).delete() }

    override fun cleanOrphans(referenced: Set<String>) {
        root.listFiles()?.filter { it.isFile && "moment_photos/${it.name}" !in referenced }
            ?.forEach { it.delete() }
    }
}
