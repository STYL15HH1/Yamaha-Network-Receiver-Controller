package com.styl15hh1.rn301controller

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
@Config(sdk=[35],qualifiers="en-rUS-w411dp-h891dp")
class V073UiTest {
    @get:Rule val compose=createComposeRule()
    private fun info(rich:Boolean=true):TunerStatus {
        var text=javaClass.getResource("/rn301-tuner-not-ready.xml")!!.readText()
            .replace("Not Ready","Ready").replace("<Tuned>Negate","<Tuned>Assert")
        if(rich)text=text.replace("<Program_Service></Program_Service>","<Program_Service>Station</Program_Service>")
            .replace("<Radio_Text_A></Radio_Text_A>","<Radio_Text_A>Programme</Radio_Text_A>")
            .replace("<Radio_Text_B></Radio_Text_B>","<Radio_Text_B>Programme</Radio_Text_B>")
            .replace("<Program_Type></Program_Type>","<Program_Type>News</Program_Type>")
            .replace("<Clock_Time></Clock_Time>","<Clock_Time>12:34</Clock_Time>")
        return YamahaXmlParser().tuner(text)
    }
    @Test fun bandAndModeRequireReceiverReadbackAndHideForAm() {
        var state by mutableStateOf(TunerState(status=info(),
            fmRange=TuningRange(8750,10800,5),amRange=TuningRange(531,1611,9,TunerBand.AM)))
        var selected:TunerBand?=null;var reception:FmMode?=null
        compose.setContent{MaterialTheme{Column{TunerOptions(state,true,{selected=it},{reception=it})}}}
        compose.onNodeWithText("FM").assertIsSelected()
        compose.onNodeWithText("AM").performClick()
        compose.runOnIdle{assertEquals(TunerBand.AM,selected)}
        compose.onNodeWithText("FM").assertIsSelected() // No optimistic change on click.
        compose.onNodeWithText("Mono").performClick()
        compose.runOnIdle{assertEquals(FmMode.MONO,reception)}
        compose.onNodeWithText("Auto").assertIsSelected()
        compose.runOnIdle{state=state.copy(status=info(false).copy(band="AM",fmMode=null))}
        compose.onNodeWithText("AM").assertIsSelected()
        compose.onNodeWithText("Reception").assertDoesNotExist()
        compose.onNodeWithText("Auto").assertDoesNotExist()
        compose.onNodeWithText("Mono").assertDoesNotExist()
    }
    @Test fun disconnectedOptionsCannotSendCommands() {
        var calls=0
        compose.setContent{MaterialTheme{Column{TunerOptions(TunerState(status=info(),
            fmRange=TuningRange(8750,10800,5),amRange=TuningRange(531,1611,9,TunerBand.AM)),false,{calls++},{calls++})}}}
        for(label in listOf("FM","AM","Auto","Mono"))compose.onNodeWithText(label).assertIsNotEnabled()
        compose.runOnIdle{assertEquals(0,calls)}
    }
    @Test fun homeUsesSharedRdsWithoutDuplicatesAndHasCompactFallback() {
        var state by mutableStateOf(info())
        compose.setContent{MaterialTheme{Column{TunerHomeInformation(state)}}}
        compose.onAllNodesWithText("Station").assertCountEquals(1)
        compose.onAllNodesWithText("Programme").assertCountEquals(1)
        compose.onNodeWithText("News").assertDoesNotExist()
        compose.onNodeWithText("97.4 MHz").assertExists()
        compose.onNodeWithText("FM · Tuned · Mono · Preset 1").assertExists()
        compose.onNodeWithText("12:34",substring=true).assertDoesNotExist()
        compose.runOnIdle{state=info(false)}
        compose.onNodeWithText("Station").assertDoesNotExist()
        compose.onNodeWithText("Programme").assertDoesNotExist()
        compose.onNodeWithText("News").assertDoesNotExist()
        compose.onNodeWithText("97.4 MHz").assertExists()
    }
    @Test fun playerOmitsClockAndPreservesActualStereoIndication() {
        var state by mutableStateOf(info())
        compose.setContent{MaterialTheme{Column{TunerInformation(state)}}}
        compose.onNodeWithText("RDS clock: 12:34").assertDoesNotExist()
        compose.onNodeWithText("Tuned · Mono",substring=true).assertExists() // FM mode is Auto, actual Stereo is Negate.
        compose.runOnIdle{state=info(false)}
        compose.onNodeWithText("RDS clock:",substring=true).assertDoesNotExist()
    }
    @Test fun bothTunerViewsDeduplicateDecimalCommaFrequencyAndRds() {
        var home by mutableStateOf(true)
        val base=info()
        var state by mutableStateOf(base.copy(nowPlaying=base.nowPlaying.copy(
            station="97,4 MHz",title="97.4 MHz"),programType="97,4 MHz"))
        compose.setContent{ReceiverTheme(true){Column{
            if(home) TunerHomeInformation(state) else TunerInformation(state)
        }}}
        for(isHome in listOf(true,false)) {
            compose.runOnIdle{home=isHome}
            compose.onAllNodesWithText("97.4 MHz").assertCountEquals(1)
            compose.onNodeWithText("97,4 MHz").assertDoesNotExist()
        }
        compose.runOnIdle{state=base.copy(nowPlaying=base.nowPlaying.copy(station="TOK FM",title="TOK FM"),programType="TOK FM")}
        compose.onAllNodesWithText("TOK FM").assertCountEquals(1)
        compose.onAllNodesWithText("97.4 MHz").assertCountEquals(1)
    }
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun homeStationPrecedesFrequencyAndRadioTextIsLimitedToTwoLines() {
        val base=info()
        val text="Long RDS information ".repeat(30)
        val state=base.copy(nowPlaying=base.nowPlaying.copy(station="TOK FM",title=text))
        compose.setContent{ReceiverTheme(true){Column{TunerHomeInformation(state)}}}
        val station=compose.onNodeWithText("TOK FM").fetchSemanticsNode().boundsInRoot
        val frequency=compose.onNodeWithText("97.4 MHz").fetchSemanticsNode().boundsInRoot
        assertTrue(station.bottom <= frequency.top)
        val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(text.trim()).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(2,layouts.single().lineCount)
        assertTrue(layouts.single().isLineEllipsized(1))
    }
    @Test fun programServiceRemainsPrimaryAndPtyIsHidden() {
        val xml=javaClass.getResource("/rn301-tuner-not-ready.xml")!!.readText()
            .replace("Not Ready","Ready")
            .replace("<Program_Service></Program_Service>","<Program_Service>TOK FM</Program_Service>")
            .replace("<Radio_Text_A></Radio_Text_A>","<Radio_Text_A>tokfm.pl</Radio_Text_A>")
            .replace("<Program_Type></Program_Type>","<Program_Type>INFO</Program_Type>")
        val state=YamahaXmlParser().tuner(xml)
        var home by mutableStateOf(true)
        compose.setContent{ReceiverTheme(true){Column{
            if(home) TunerHomeInformation(state) else TunerInformation(state)
        }}}
        for(isHome in listOf(true,false)) {
            compose.runOnIdle{home=isHome}
            val station=compose.onNodeWithText("TOK FM").fetchSemanticsNode().boundsInRoot
            val radioText=compose.onNodeWithText("tokfm.pl").fetchSemanticsNode().boundsInRoot
            assertTrue(station.bottom <= radioText.top)
            compose.onAllNodesWithText("TOK FM").assertCountEquals(1)
            compose.onNodeWithText("INFO").assertDoesNotExist()
        }
    }
    @Test fun receiverInformationShowsFirmwareOrUnavailableWithoutNetwork() {
        var state by mutableStateOf(ReceiverStatus(modelName="R-N301",address="receiver",
            connectionState=ConnectionState.CONNECTED,firmwareVersion="1.13/0.05"))
        compose.setContent{MaterialTheme{Column{ReceiverInformation(state)}}}
        compose.onNodeWithText("Receiver information").assertExists()
        compose.onNodeWithText("R-N301").assertExists()
        compose.onNodeWithText("receiver").assertExists()
        compose.onNodeWithText("Firmware: 1.13/0.05").assertExists()
        compose.runOnIdle{state=state.copy(firmwareVersion=null)}
        compose.onNodeWithText("Firmware: Unavailable").assertExists()
    }
}
