package com.example.assignment03.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.assignment03.model.JournalEntry
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Prepares an attachment off the main thread; the caller launches the chooser. */
suspend fun prepareMomentShare(context: Context, entry: JournalEntry): Intent =
    withContext(Dispatchers.IO) {
        val text = listOfNotNull(entry.title, entry.content)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        val intent = Intent(Intent.ACTION_SEND)
        if (text.isNotBlank()) intent.putExtra(Intent.EXTRA_TEXT, text)

        val photo = entry.photoUri
        if (photo.isNullOrBlank()) {
            require(text.isNotBlank()) { "Moment has no shareable content" }
            intent.type = "text/plain"
            return@withContext intent
        }

        val source = Uri.parse(photo)
        require(source.scheme in setOf("content", "file", "android.resource")) {
            "Unsupported photo URI"
        }
        val directory = File(context.cacheDir, "shared_images")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create share cache" }
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory.listFiles()?.filter { it.isFile && it.lastModified() < cutoff }
            ?.forEach { it.delete() }

        val resolver = context.contentResolver
        val detectedType = resolver.getType(source)?.takeIf { it.startsWith("image/") }
            ?: resolver.openInputStream(source)?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                options.outMimeType
            }
        require(detectedType?.startsWith("image/") == true) { "Photo is not a readable image" }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(detectedType)
            ?: "img"
        val attachment = File.createTempFile("moment-", ".$extension", directory)
        try {
            resolver.openInputStream(source)?.use { input ->
                attachment.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Cannot open photo")
            check(attachment.length() > 0) { "Photo is empty" }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", attachment
            )
            intent.apply {
                type = detectedType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(resolver, "Moment photo", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (error: Exception) {
            attachment.delete()
            throw error
        }
    }
