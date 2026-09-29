package com.styl15hh1.rn301controller

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import com.styl15hh1.rn301controller.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="en-rUS-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VolumeValuePolishTest {
    @get:Rule val compose=createComposeRule()
    @Test fun strongerNativeValueFitsThreeDigitsAndLargeFontScale() {
        var value by mutableStateOf("42")
        var scale by mutableFloatStateOf(1f)
        compose.setContent { ReceiverTheme(true) {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {
                Box(Modifier.width(120.dp)) { LargeVolumeValue(value) }
            }
        }}
        fun layout():TextLayoutResult {
            val result=mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(value).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(result)}
            return result.single()
        }
        assertTrue(layout().layoutInput.style.fontSize.value > 57f)
        assertEquals(FontWeight.SemiBold,layout().layoutInput.style.fontWeight)
        for(fontScale in listOf(1f,2f)) {
            compose.runOnIdle { value="100";scale=fontScale }
            val result=layout()
            assertFalse("scale=$fontScale size="+result.size+" width="+result.multiParagraph.width+" height="+result.multiParagraph.height+" font="+result.layoutInput.style.fontSize+" constraints="+result.layoutInput.constraints, result.hasVisualOverflow)
            assertEquals(1,result.lineCount)
            compose.onNodeWithText("100").assertIsDisplayed()
        }
    }
}
