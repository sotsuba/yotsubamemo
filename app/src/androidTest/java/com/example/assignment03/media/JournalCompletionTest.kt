package com.example.assignment03.media

import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.assignment03.MainActivity
import com.example.assignment03.data.JournalContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class JournalCompletionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val marker = "complete-${UUID.randomUUID()}"
    private val repository get() = JournalContainer.get(compose.activity).repository

    @Before fun setup() {
        compose.activityRule.scenario.onActivity {
            it.setTurnScreenOn(true)
            it.setShowWhenLocked(true)
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @After fun cleanup() {
        runBlocking {
            repository.observeEntries().first().filter { it.title?.startsWith(marker) == true }
                .forEach { repository.delete(it.id) }
        }
    }

    private fun waitFor(text: String) {
        compose.waitUntil(10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun textNoteEditSearchAndDelete() {
        compose.onNodeWithContentDescription("Add moment").performClick()
        compose.onNodeWithText("Write a note").performClick()
        waitFor("New Moment")
        compose.onNodeWithText("Save Moment").assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput(marker)
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("Original content")
        closeSoftKeyboard()
        compose.onNodeWithText("Save Moment").performClick()
        waitFor(marker)
        val original = runBlocking { repository.observeEntries().first().single { it.title == marker } }
        assertNull(original.photoUri)
        compose.onNodeWithText(marker).performClick()
        waitFor("Your Moment")
        compose.onNodeWithContentDescription("Edit").performClick()
        waitFor("Edit Moment")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("Discard me")
        closeSoftKeyboard()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onAllNodes(hasSetTextAction())[1].assertTextContains("Discard me")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard").performClick()
        waitFor("Your Moment")
        compose.onNodeWithText("Original content").assertExists()
        compose.onNodeWithContentDescription("Edit").performClick()
        waitFor("Edit Moment")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("Updated content")
        closeSoftKeyboard()
        compose.activityRule.scenario.recreate()
        waitFor("Edit Moment")
        compose.onAllNodes(hasSetTextAction())[1].assertTextContains("Updated content")
        compose.onNodeWithText("Save Moment").performClick()
        waitFor("Your Moment")
        compose.onNodeWithText("Updated content").assertExists()
        val updated = runBlocking { repository.observeEntry(original.id).first()!! }
        assertEquals(original.createdAt, updated.createdAt)
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithText("Search moments").performTextInput("  ${marker.uppercase()}  ")
        closeSoftKeyboard()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Search moments").assertTextContains("  ${marker.uppercase()}  ")
        compose.onNodeWithText(marker).assertExists()
        compose.onNodeWithText("Search moments").performTextReplacement("no-match-$marker")
        compose.onNodeWithText("No matching moments.").assertExists()
        compose.onNodeWithText("Clear").performClick()
        closeSoftKeyboard()
        compose.onNodeWithText(marker).performClick()
        waitFor("Your Moment")
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText(marker).assertExists()
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.onNodeWithText("Delete").performClick()
        try {
            compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Add moment").fetchSemanticsNodes().isNotEmpty() }
        } catch (error: Throwable) {
            throw AssertionError(compose.onRoot().printToString(), error)
        }
        assertNull(runBlocking { repository.observeEntry(original.id).first() })
        compose.onNodeWithText(marker).assertDoesNotExist()
    }

    @Test fun appearancePersistsAfterRecreation() {
        val prefs = compose.activity.getSharedPreferences("memo_settings", 0)
        val before = prefs.getString("theme", "SYSTEM")!!
        try {
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("Dark").performClick()
            compose.onNodeWithText("Dark").assertIsSelected()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("Dark").assertIsSelected()
            assertEquals("DARK", prefs.getString("theme", null))
            compose.onNodeWithText("Light").performClick()
            compose.onNodeWithText("Light").assertIsSelected()
            compose.onNodeWithText("System").performClick()
            compose.onNodeWithText("System").assertIsSelected()
        } finally {
            compose.onNodeWithText(before.lowercase().replaceFirstChar { it.uppercase() }).performClick()
        }
    }
}
