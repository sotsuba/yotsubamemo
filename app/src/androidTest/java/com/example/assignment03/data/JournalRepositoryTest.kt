package com.example.assignment03.data

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.assignment03.R
import java.io.File
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: JournalDatabase
    private lateinit var photos: InternalPhotoStore
    private lateinit var repository: JournalRepository
    private var now = 100L
    private val dbName = "journal-repository-test.db"

    @Before fun setup() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(base.filesDir, "repository-tests").apply { mkdirs() }
        }
        context.deleteDatabase(dbName)
        File(context.filesDir, "moment_photos").deleteRecursively()
        db = openDatabase()
        photos = InternalPhotoStore(context)
        repository = JournalRepository(db.journalDao(), photos, clock = { now })
    }

    @After fun teardown() {
        db.close()
        context.deleteDatabase(dbName)
        File(context.filesDir, "moment_photos").deleteRecursively()
    }

    private fun openDatabase() = Room.databaseBuilder(context, JournalDatabase::class.java, dbName).build()
    private fun sample() = Uri.parse("android.resource://${context.packageName}/${R.drawable.sample_rainy}")
    private suspend fun entry(id: Long) = repository.observeEntry(id).first()!!
    private suspend fun fails(block: suspend () -> Unit) {
        try { block(); fail("Expected failure") } catch (_: Exception) { }
    }

    @Test fun crudNormalizationOrderingAndReopen() = runBlocking {
        val first = repository.create(JournalInput(content = "  Xin chào 🍀\nDòng hai  "))
        val second = repository.create(JournalInput(title = " Tiêu đề ", mood = JournalMood.HAPPY))
        assertTrue(second > first)
        assertEquals(listOf(second, first), repository.observeEntries().first().map { it.id })
        assertEquals("Xin chào 🍀\nDòng hai", entry(first).content)
        assertNull(entry(first).title)
        assertEquals("Happy", entry(second).mood)
        now = 200L
        repository.update(first, JournalInput(content = "Updated"))
        assertEquals(100L, entry(first).createdAt)
        assertEquals(200L, entry(first).updatedAt)
        db.close()
        db = openDatabase()
        repository = JournalRepository(db.journalDao(), photos)
        assertEquals("Updated", entry(first).content)
        repository.delete(first)
        repository.delete(first)
        assertNull(repository.observeEntry(first).first())
        fails { repository.update(first, JournalInput(content = "Missing")) }
    }

    @Test fun flowEmitsCommittedChanges() = runBlocking {
        val emission = async {
            withTimeout(5000) { repository.observeEntries().first { it.any { row -> row.content == "New" } } }
        }
        val id = repository.create(JournalInput(content = "New"))
        assertEquals(id, emission.await().single().id)
    }

    @Test fun rejectsEmptyMoments() = runBlocking {
        fails { repository.create(JournalInput(title = " ", content = "\n", mood = JournalMood.PEACEFUL)) }
        val id = repository.create(JournalInput(content = "Keep"))
        fails { repository.update(id, JournalInput()) }
        assertEquals("Keep", entry(id).content)
    }

    @Test fun importsReplacesRemovesAndDeletesPhotos() = runBlocking {
        val id = repository.create(JournalInput(), sample())
        val original = File(Uri.parse(entry(id).photoUri).path!!)
        assertTrue(original.isFile)
        repository.update(id, JournalInput(content = "Keep"))
        assertTrue(original.isFile)
        repository.update(id, JournalInput(), PhotoChange.Replace(sample()))
        val replacement = File(Uri.parse(entry(id).photoUri).path!!)
        assertFalse(original.exists())
        assertTrue(replacement.exists())
        fails { repository.update(id, JournalInput(), PhotoChange.Remove) }
        repository.update(id, JournalInput(content = "Text"), PhotoChange.Remove)
        assertFalse(replacement.exists())
        assertNull(entry(id).photoUri)
        repository.update(id, JournalInput(), PhotoChange.Replace(sample()))
        val last = File(Uri.parse(entry(id).photoUri).path!!)
        repository.delete(id)
        assertFalse(last.exists())
    }

    @Test fun missingPhotoAndStartupCleanup() = runBlocking {
        val id = repository.create(JournalInput(content = "Survives"), sample())
        val managed = File(Uri.parse(entry(id).photoUri).path!!)
        val orphan = File(managed.parentFile, "orphan.tmp").apply { writeText("partial") }
        repository = JournalRepository(db.journalDao(), photos)
        repository.initialize()
        assertFalse(orphan.exists())
        assertTrue(managed.exists())
        managed.delete()
        assertEquals("Survives", entry(id).content)
        assertNull(entry(id).photoUri)
    }

    @Test fun invalidAndUnreadableSourcesPreserveExistingData() = runBlocking {
        val id = repository.create(JournalInput(content = "Old"), sample())
        val original = entry(id)
        val invalid = File(context.cacheDir, "invalid-photo.txt").apply { writeText("not an image") }
        try {
            for (uri in listOf(Uri.fromFile(invalid), Uri.parse("content://missing.provider/photo"))) {
                fails { repository.update(id, JournalInput(content = "New"), PhotoChange.Replace(uri)) }
                assertEquals(original, entry(id))
            }
            assertEquals(1, File(context.filesDir, "moment_photos").listFiles()!!.size)
        } finally { invalid.delete() }
    }

    @Test fun databaseFailureRollsBackNewPhotoAndKeepsOldPhoto() = runBlocking {
        val id = repository.create(JournalInput(content = "Old"), sample())
        val original = entry(id)
        val failingDao = object : JournalDao by db.journalDao() {
            override suspend fun insert(entry: JournalEntryEntity): Long = throw IOException("Insert failed")
            override suspend fun update(entry: JournalEntryEntity): Int = throw IOException("Update failed")
        }
        val failing = JournalRepository(failingDao, photos)
        fails { failing.create(JournalInput(), sample()) }
        fails { failing.update(id, JournalInput(content = "New"), PhotoChange.Replace(sample())) }
        assertEquals(original, entry(id))
        assertEquals(1, File(context.filesDir, "moment_photos").listFiles()!!.size)
    }

    @Test fun fileWriteFailureDoesNotChangeDatabase() = runBlocking {
        val id = repository.create(JournalInput(content = "Old"))
        val failing = JournalRepository(db.journalDao(), object : PhotoStore by photos {
            override fun importPhoto(uri: Uri): String = throw IOException("Disk full")
        })
        fails { failing.update(id, JournalInput(content = "New"), PhotoChange.Replace(sample())) }
        assertEquals("Old", entry(id).content)
        assertNull(entry(id).photoUri)
    }
}
