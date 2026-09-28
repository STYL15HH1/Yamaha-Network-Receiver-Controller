package com.styl15hh1.rn301controller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.YamahaXmlParser
import com.styl15hh1.rn301controller.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="en-rUS-w411dp-h891dp")
class V072UiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun oneTwoThreeFavoritesAreCenteredTallOrderedAndSelected() {
        var sources by mutableStateOf(listOf(Source("TUNER")))
        compose.setContent { MaterialTheme { QuickSourceRow(sources,Source("TUNER"),true) {} } }
        for(ids in listOf(listOf("TUNER"),listOf("OPTICAL","TUNER"),listOf("OPTICAL","TUNER","CD"))) {
            compose.runOnIdle { sources=ids.map {Source(it)} }
            val bounds=ids.map { compose.onNodeWithText(Source(it).title).fetchSemanticsNode().boundsInRoot }
            val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
            assertEquals(root.center.x,(bounds.first().left+bounds.last().right)/2,1f)
            bounds.zipWithNext().forEach {(a,b)->assertTrue(a.left<b.left);assertEquals(a.center.y,b.center.y,1f)}
            compose.onNodeWithText("Tuner").assertIsSelected().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(68f))
        }
    }
    @Test fun presetsAreEqualSizedNumbersWithSelectionSemantics() {
        compose.setContent { MaterialTheme {
            PresetGrid(listOf(PresetPresentation(1,"Station","101.2 MHz",false),
                PresetPresentation(2,"Long station name","95.5 MHz",true),
                PresetPresentation(3,null,null,false)),true) {}
        } }
        val sizes=(1..3).map {compose.onNodeWithText(it.toString()).fetchSemanticsNode().boundsInRoot}
        sizes.zipWithNext().forEach { (a,b) -> assertEquals(a.width,b.width,1f);assertEquals(a.height,b.height,0f) }
        compose.onNodeWithText("2").assertIsSelected()
        compose.onNodeWithText("Selected").assertDoesNotExist()
        compose.onNodeWithText("101.2 MHz").assertDoesNotExist()
        compose.onNodeWithText("Preset 2").assertDoesNotExist()
        compose.onNodeWithContentDescription("Preset 2").assertExists()
    }
    @Test fun tunerHeaderShowsActualMetadataWithoutRepeatingPresetDetails() {
        val xml=javaClass.getResource("/rn301-tuner-not-ready.xml")!!.readText()
            .replace("Not Ready","Ready").replace("<Tuned>Negate</Tuned>","<Tuned>Assert</Tuned>")
            .replace("<Program_Service></Program_Service>","<Program_Service>Station</Program_Service>")
            .replace("<Radio_Text_A></Radio_Text_A>","<Radio_Text_A>Programme</Radio_Text_A>")
            .replace("<Radio_Text_B></Radio_Text_B>","<Radio_Text_B>Presenter</Radio_Text_B>")
            .replace("<Program_Type></Program_Type>","<Program_Type>Culture</Program_Type>")
        var info by mutableStateOf(YamahaXmlParser().tuner(xml))
        compose.setContent { MaterialTheme { Column { TunerInformation(info) } } }
        for(text in listOf("Station","97.4 MHz","FM","Preset 1","Culture"))compose.onNodeWithText(text,substring=true).assertExists()
        compose.onNodeWithText("Programme",substring=true).assertExists()
        compose.onNodeWithText("Presenter",substring=true).assertExists()
        compose.onNodeWithText("Tuned · Mono",substring=true).assertExists()
        compose.runOnIdle { info=YamahaXmlParser().tuner(javaClass.getResource("/rn301-tuner-not-ready.xml")!!.readText()) }
        compose.onNodeWithText("Station").assertDoesNotExist()
        compose.onNodeWithText("Culture").assertDoesNotExist()
    }
    @Test fun globalConnectionActionOnlyAppearsWhenDisconnected() {
        var state by mutableStateOf(ReceiverStatus())
        var connects=0;var settings=0
        compose.setContent { MaterialTheme { ReceiverHeader(state,false,{connects++},{},{settings++}) } }
        compose.onNodeWithContentDescription("Connect your receiver").performClick()
        compose.runOnIdle {
            assertEquals(1,connects)
            state=ReceiverStatus(connectionState=ConnectionState.CONNECTED,address="receiver",modelName="R-N301",powerState=PowerState.ON)
        }
        compose.onNodeWithContentDescription("Connect your receiver").assertDoesNotExist()
        compose.onNodeWithText("Connected",substring=true).assertExists()
        compose.onNodeWithContentDescription("Standby").assertIsEnabled()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.runOnIdle {assertEquals(1,settings)}
    }
}
