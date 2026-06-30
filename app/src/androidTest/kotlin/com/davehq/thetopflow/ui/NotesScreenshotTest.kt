package com.davehq.thetopflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davehq.thetopflow.data.NoteUi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotesScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noteCard_dark_fontScale1() = captureNoteCard(darkTheme = true, fontScale = 1.0f)

    @Test
    fun noteCard_dark_fontScale13() = captureNoteCard(darkTheme = true, fontScale = 1.3f)

    @Test
    fun noteCard_light_fontScale1() = captureNoteCard(darkTheme = false, fontScale = 1.0f)

    @Test
    fun noteCard_light_fontScale13() = captureNoteCard(darkTheme = false, fontScale = 1.3f)

    @Test
    fun editor_dark_fontScale1() = captureEditor(darkTheme = true, fontScale = 1.0f)

    @Test
    fun editor_dark_fontScale13() = captureEditor(darkTheme = true, fontScale = 1.3f)

    @Test
    fun editor_light_fontScale1() = captureEditor(darkTheme = false, fontScale = 1.0f)

    @Test
    fun editor_light_fontScale13() = captureEditor(darkTheme = false, fontScale = 1.3f)

    private fun captureNoteCard(darkTheme: Boolean, fontScale: Float) {
        composeRule.setContent {
            val density = LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                NotesTheme(darkTheme = darkTheme, dynamicColor = false) {
                    Box(Modifier.padding(24.dp)) {
                        NoteCard(note = sampleNote, selected = false)
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureToImage().assertNonBlank()
    }

    private fun captureEditor(darkTheme: Boolean, fontScale: Float) {
        composeRule.setContent {
            val density = LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                NotesTheme(darkTheme = darkTheme, dynamicColor = false) {
                    NoteEditor(
                        note = sampleNote,
                        isCreating = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureToImage().assertNonBlank()
    }

    private fun androidx.compose.ui.graphics.ImageBitmap.assertNonBlank() {
        assertTrue(width > 0)
        assertTrue(height > 0)
    }

    private val sampleNote = NoteUi(
        id = "screenshot-note",
        title = "Material motion draft",
        body = "Pixel-grade note text should stay sharp at rest, scale cleanly with font settings, and avoid bitmap or canvas rendering.",
        font = "sans",
        fontSizeSp = 18,
        noteColor = 0xFF0ECDBE.toInt(),
        textColor = 0xFF080C0E.toInt(),
        accentColor = 0xFF84FFEE.toInt(),
        noteGlow = false,
        glowStrength = 1,
        songUri = "",
        recordings = emptyList(),
        createdAt = 1_788_000_000_000,
        updatedAt = 1_788_000_000_000
    )
}
