package com.example.assignment03.media

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.WindowManager
import androidx.compose.ui.semantics.SemanticsActions
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.assignment03.MainActivity
import com.example.assignment03.R
import com.example.assignment03.data.JournalContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhotoFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val marker = "flow-test-${UUID.randomUUID()}"
    private val context get() = compose.activity
    private val repository get() = JournalContainer.get(context).repository
    private fun source() = Uri.parse("android.resource://${context.packageName}/${R.drawable.sample_rainy}")

    @Before fun setup() {
        compose.activityRule.scenario.onActivity {
            it.setTurnScreenOn(true)
            it.setShowWhenLocked(true)
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        Intents.init()
    }
    @After fun teardown() {
        Intents.release()
        runBlocking {
            repository.observeEntries().first().filter { it.title?.startsWith(marker) == true }
                .forEach { repository.delete(it.id) }
        }
    }

    private fun waitFor(text: String) {
        compose.waitUntil(10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun choose() {
        Intents.intending(hasAction(Intent.ACTION_GET_CONTENT))
            .respondWith(ActivityResult(Activity.RESULT_OK, Intent().setData(source())))
        compose.onNodeWithContentDescription("Add moment").performClick()
        compose.onNodeWithText("Choose photo").performClick()
        waitFor("New Moment")
    }
    private fun fillAndSave() {
        compose.onAllNodes(hasSetTextAction())[0].performTextInput(marker)
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("Nội dung thật 🍀\nDòng thứ hai")
        closeSoftKeyboard()
        val click = compose.onNodeWithText("Save Moment").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle { click(); click() } // Repeated taps must create only one row.
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Add moment").fetchSemanticsNodes().isNotEmpty() }
        waitFor(marker)
        runBlocking {
            val saved = repository.observeEntries().first().single { it.title == marker }
            assertEquals("Nội dung thật 🍀\nDòng thứ hai", saved.content)
            val bytes = context.contentResolver.openInputStream(Uri.parse(saved.photoUri))!!.use { it.readBytes() }
            val expected = context.contentResolver.openInputStream(source())!!.use { it.readBytes() }
            assertArrayEquals(expected, bytes)
        }
        compose.onNodeWithText(marker).performClick()
        waitFor("Your Moment")
        compose.onNodeWithText("Nội dung thật 🍀\nDòng thứ hai").assertExists()
    }

    @Test fun cameraReturnsFullPhotoThenSavesRealMoment() {
        Intents.intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWithFunction { intent ->
            @Suppress("DEPRECATION")
            val output = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
            assertEquals("content", output.scheme)
            assertEquals("${context.packageName}.fileprovider", output.authority)
            context.contentResolver.openInputStream(source())!!.use { input ->
                context.contentResolver.openOutputStream(output)!!.use { input.copyTo(it) }
            }
            ActivityResult(Activity.RESULT_OK, null)
        }
        compose.onNodeWithContentDescription("Camera").performClick()
        waitFor("New Moment")
        Intents.intended(hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
        fillAndSave()
    }

    @Test fun pickerReplacementAndRecreationKeepDraftAndSave() {
        choose()
        Intents.intended(allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("image/*"),
            hasCategories(setOf(Intent.CATEGORY_OPENABLE))))
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Temporary title")
        closeSoftKeyboard()
        compose.onNodeWithContentDescription("Journal photo").performClick()
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        waitFor("New Moment")
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Temporary title")
        compose.onAllNodes(hasSetTextAction())[0].performTextClearance()
        fillAndSave()
        compose.activityRule.scenario.recreate()
        waitFor("Your Moment")
        compose.onNodeWithText(marker).assertExists()
    }

    @Test fun cancellationsDoNotCreateMoments() {
        val before = runBlocking { repository.observeEntries().first().map { it.id } }
        Intents.intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
        Intents.intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
        compose.onNodeWithContentDescription("Camera").performClick()
        compose.waitForIdle()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Add moment").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Add moment").performClick()
        compose.onNodeWithText("Choose photo").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("New Moment").assertDoesNotExist()
        assertEquals(before, runBlocking { repository.observeEntries().first().map { it.id } })
    }

    @Test fun cancelledReplacementPreservesTextAndPhoto() {
        choose()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput(marker)
        closeSoftKeyboard()
        Intents.intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
        compose.onNodeWithContentDescription("Journal photo").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains(marker)
        compose.onNodeWithContentDescription("Journal photo").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard").performClick()
        compose.onNodeWithText("New Moment").assertDoesNotExist()
        assertFalse(runBlocking { repository.observeEntries().first().any { it.title == marker } })
    }

    @Test fun invalidImageDoesNotOpenEditor() {
        Intents.intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(ActivityResult(Activity.RESULT_OK,
            Intent().setData(Uri.parse("content://missing.provider/photo"))))
        compose.onNodeWithContentDescription("Add moment").performClick()
        compose.onNodeWithText("Choose photo").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Add moment").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("New Moment").assertDoesNotExist()
    }

    @Test fun failedSaveKeepsDraftAndCanRetryWithNewPhoto() {
        val directory = File(context.cacheDir, "draft_images")
        val before = directory.listFiles()?.map { it.name }?.toSet().orEmpty()
        choose()
        val selected = directory.listFiles()!!.single { it.name !in before }
        selected.delete() // Simulate the cache image disappearing before Save.
        compose.onAllNodes(hasSetTextAction())[0].performTextInput(marker)
        closeSoftKeyboard()
        compose.onNodeWithText("Save Moment").performClick()
        waitFor("Could not save this moment. Your draft is still here. Please try again.")
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains(marker)
        assertFalse(runBlocking { repository.observeEntries().first().any { it.title == marker } })
        compose.onNodeWithContentDescription("Journal photo").performClick()
        compose.waitUntil(10000) { directory.listFiles()?.any { it.name !in before } == true }
        compose.waitForIdle()
        compose.onNodeWithText("Save Moment").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Add moment").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, runBlocking { repository.observeEntries().first().count { it.title == marker } })
    }

    @Test fun editPhotoKeepReplaceDiscardAndRemove() {
        choose()
        fillAndSave()
        val original = runBlocking { repository.observeEntries().first().single { it.title == marker } }
        fun saved() = runBlocking { repository.observeEntry(original.id).first()!! }
        fun edit() {
            compose.onNodeWithContentDescription("Edit").performClick()
            waitFor("Edit Moment")
        }
        edit()
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("Edited text")
        closeSoftKeyboard()
        compose.onNodeWithText("Save Moment").performClick()
        waitFor("Your Moment")
        assertEquals(original.photoUri, saved().photoUri)
        assertEquals(original.createdAt, saved().createdAt)

        edit()
        compose.onNodeWithContentDescription("Journal photo").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard").performClick()
        waitFor("Your Moment")
        assertEquals(original.photoUri, saved().photoUri)
        assertTrue(context.contentResolver.openInputStream(Uri.parse(original.photoUri))!!.use { it.read() } >= 0)

        edit()
        compose.onNodeWithContentDescription("Journal photo").performClick()
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        waitFor("Edit Moment")
        compose.onNodeWithText("Save Moment").performClick()
        waitFor("Your Moment")
        val replaced = saved()
        assertNotEquals(original.photoUri, replaced.photoUri)
        assertFalse(File(Uri.parse(original.photoUri).path!!).exists())

        edit()
        compose.onNodeWithText("Remove photo").performScrollTo().performClick()
        compose.onNodeWithText("Save Moment").performClick()
        waitFor("Your Moment")
        assertNull(saved().photoUri)
        assertFalse(File(Uri.parse(replaced.photoUri).path!!).exists())
    }

    @Test fun settingsAndJournalNavigationRemainUsable() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Add moment").assertDoesNotExist()
        compose.onNodeWithContentDescription("Journal").performClick()
        compose.onNodeWithContentDescription("Add moment").assertExists()
    }
}
