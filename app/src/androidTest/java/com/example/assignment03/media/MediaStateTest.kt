package com.example.assignment03.media

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.assignment03.R
import com.example.assignment03.ui.state.EntryEditorViewModel
import com.example.assignment03.ui.state.MediaViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaStateTest {
    @Test fun pendingCameraResultSurvivesSavedStateRestoration() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val handle = SavedStateHandle()
        val output = CompletableDeferred<Uri>()
        withContext(Dispatchers.Main) { MediaViewModel(app, handle).startCamera { output.complete(it) } }
        val uri = withTimeout(5000) { output.await() }
        val name = handle.get<String>("pendingCamera")!!
        val photos = DraftPhotos(app)
        try {
            app.resources.openRawResource(R.drawable.sample_rainy).use { input ->
                app.contentResolver.openOutputStream(uri)!!.use { input.copyTo(it) }
            }
            val restored = withContext(Dispatchers.Main) {
                val snapshot = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
                MediaViewModel(app, snapshot).also { it.cameraResult(true) }
            }
            assertEquals(name, withTimeout(5000) { restored.ready.filterNotNull().first() })
            photos.validate(name)
        } finally { photos.delete(name) }
    }

    @Test fun editorRestoresReplacementAndTextWithoutReinitializing() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        withContext(Dispatchers.Main) {
            val original = SavedStateHandle()
            val editor = EntryEditorViewModel(app, original)
            editor.initialize(null)
            editor.titleChanged("Tiêu đề")
            editor.contentChanged("Dòng một\nDòng hai")
            editor.moodChanged("Happy")
            val restored = EntryEditorViewModel(app, SavedStateHandle(original.keys().associateWith { original.get<Any?>(it) }))
            restored.initialize("ignored-original-route.jpg")
            assertNull(restored.photo.value)
            assertEquals("Tiêu đề", restored.title.value)
            assertEquals("Dòng một\nDòng hai", restored.content.value)
            assertEquals("Happy", restored.mood.value)
        }
    }
}
