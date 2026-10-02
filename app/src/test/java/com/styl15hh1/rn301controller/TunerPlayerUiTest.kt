package com.styl15hh1.rn301controller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TunerPlayerUiTest {
    @get:Rule val compose = createComposeRule()
    private val radioText = "TOK + Muzyka bez reklam w aplikacji ".repeat(12).trim()
    private val state = TunerState(
        status = TunerStatus(band = "FM", preset = 6, availability = "Ready", tuned = true,
            stereo = false, fmMode = FmMode.AUTO, frequency = ReceiverFrequency(9740, 2, "MHz"),
            programType = "INFO", clockTime = "12:34",
            nowPlaying = NowPlaying("TUNER", station = "TOK FM", title = radioText)),
        presets = listOf(TunerPreset(6, "6 : FM 97.40 MHz")), presetsLoaded = true,
        fmRange = TuningRange(8750, 10800, 5), amRange = TuningRange(531, 1611, 9, TunerBand.AM))

    @Test fun compactPlayerLayoutAndControls() {
        var density = 1f
        var enabled by mutableStateOf(true)
        var tuner by mutableStateOf(state)
        val steps = mutableListOf<Int>()
        val bands = mutableListOf<TunerBand>()
        val modes = mutableListOf<FmMode>()
        compose.setContent { MaterialTheme {
            density = LocalDensity.current.density
            Box(Modifier.width(379.dp).testTag("player")) {
                TunerPlayer(tuner, enabled, { bands.add(it) }, { modes.add(it) }, { steps.add(it) })
            }
        } }
        val height = compose.onNodeWithTag("player").fetchSemanticsNode().boundsInRoot.height / density
        println("Tuner player height: $height dp")
        assertTrue("Player should be substantially shorter than the 407 dp baseline", height < 260f)
        val station = compose.onNodeWithText("TOK FM").fetchSemanticsNode().boundsInRoot
        val frequency = compose.onNodeWithText("97.4 MHz").fetchSemanticsNode().boundsInRoot
        assertEquals(station.center.y, frequency.center.y, 1f)
        assertTrue(station.right <= frequency.left)
        compose.onNodeWithText("FM · Preset 6 · Tuned · Mono").assertExists()
        compose.onNodeWithText("INFO").assertDoesNotExist()
        compose.onNodeWithText("12:34", substring = true).assertDoesNotExist()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(radioText).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        assertTrue(layouts.single().isLineEllipsized(0))
        val chips = listOf("FM", "AM", "Auto", "Mono")
        val chipBounds = chips.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        chipBounds.zipWithNext().forEach { (a, b) -> assertEquals(a.center.y, b.center.y, 1f) }
        compose.onNodeWithText("FM").assertIsSelected()
        compose.onNodeWithText("Auto").assertIsSelected()
        chips.forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("Previous").performClick()
        compose.onNodeWithText("Next").performClick()
        compose.runOnIdle {
            assertEquals(listOf(TunerBand.FM, TunerBand.AM), bands)
            assertEquals(listOf(FmMode.AUTO, FmMode.MONO), modes)
            assertEquals(listOf(-1, 1), steps)
            tuner = state.copy(presets = emptyList())
        }
        compose.onNodeWithText("Previous").assertIsNotEnabled()
        compose.onNodeWithText("Next").assertIsNotEnabled()
        compose.runOnIdle { enabled = false }
        chips.forEach { compose.onNodeWithText(it).assertIsNotEnabled() }
    }
}
