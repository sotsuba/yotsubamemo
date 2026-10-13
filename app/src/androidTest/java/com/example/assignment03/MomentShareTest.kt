package com.example.assignment03

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.assignment03.model.JournalEntry
import com.example.assignment03.sharing.prepareMomentShare
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MomentShareTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val photo = "android.resource://${context.packageName}/${R.drawable.sample_rainy}"

    @Test
    fun imageAndTextHaveReadableAttachmentAndReadGrant() = runBlocking {
        val intent = prepareMomentShare(context, JournalEntry(
            title = "Rainy afternoon", content = "A quiet day.", photoUri = photo
        ))
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/jpeg", intent.type)
        assertEquals("Rainy afternoon\n\nA quiet day.", intent.getStringExtra(Intent.EXTRA_TEXT))
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.fileprovider", uri.authority)
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val original = context.contentResolver.openInputStream(Uri.parse(photo))!!.use { it.readBytes() }
        val shared = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertArrayEquals(original, shared)
    }

    @Test
    fun textOnlyOmitsAttachmentAndBlankTitle() = runBlocking {
        val intent = prepareMomentShare(context, JournalEntry(title = " ", content = "My note"))
        assertEquals("text/plain", intent.type)
        assertEquals("My note", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertFalse(intent.hasExtra(Intent.EXTRA_STREAM))
        assertNull(intent.clipData)
    }

    @Test
    fun photoOnlySupportsLocalFilesAndProviderUris() = runBlocking {
        val local = File.createTempFile("share-test-", ".jpg", context.cacheDir)
        try {
            context.contentResolver.openInputStream(Uri.parse(photo))!!.use { input ->
                local.outputStream().use { input.copyTo(it) }
            }
            val intent = prepareMomentShare(context, JournalEntry(photoUri = Uri.fromFile(local).toString()))
            assertEquals("image/jpeg", intent.type)
            assertFalse(intent.hasExtra(Intent.EXTRA_TEXT))
            val providerUri = intent.clipData!!.getItemAt(0).uri
            val copied = prepareMomentShare(context, JournalEntry(photoUri = providerUri.toString()))
            assertEquals("image/jpeg", copied.type)
        } finally {
            local.delete()
        }
    }

    @Test
    fun missingPhotoAndEmptyMomentFailInsteadOfSharingPartialContent() = runBlocking {
        for (entry in listOf(
            JournalEntry(),
            JournalEntry(content = "Note", photoUri = "file://${context.cacheDir}/missing-photo.jpg")
        )) {
            val result = runCatching { prepareMomentShare(context, entry) }
            assertTrue(result.isFailure)
        }
    }
}
