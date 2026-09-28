package com.styl15hh1.rn301controller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="en-rUS-w411dp-h891dp")
class V076UiTest {
    @get:Rule val compose=createComposeRule()
    private val countryNames=listOf("Argentina","Austria","Belgium","Canada","Denmark","Egypt","France",
        "Germany","India","Japan","Mexico","Poland","Portugal","Spain","Sweden","Turkey")
    private fun menu(names:List<String> = countryNames, title:String="Countries", attribute:String="Container") =
        names.mapIndexed{i,name->MediaItem(i%8+1,name,attribute,"NET RADIO",i/8+1)}.let {
            MediaList("NET RADIO","Ready",2,title,1,it.size,it,true)
        }
    private var calls=0
    private var selected:MediaItem?=null
    private fun browser(list:MediaList) {
        compose.setContent{ReceiverTheme(true){Surface{
            MediaBrowserContent(BrowserState("NET RADIO",BrowserPhase.CONTENT,list),true,
                {calls++},{calls++},{calls++},{selected=it;calls++},{calls++},{calls++},{calls++},Modifier.fillMaxSize())
        }}}
    }
    @Test fun volumeHeaderUsesOneRowAndPersistsWhileReceiverValuesUpdate() {
        val store=MemorySettingsStore();val settings=AppSettings(store)
        var state by mutableStateOf(ReceiverStatus(volume=Volume(39,0,""),muted=false))
        compose.setContent{val prefs by settings.state.collectAsState();ReceiverTheme(true){
            ExpandableVolume(state,prefs.volumeExpanded,settings::volumeExpanded){Text("Expanded controls")}
        }}
        fun row(summary:String) {
            val title=compose.onNodeWithText("Volume",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val status=compose.onNodeWithText(summary,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertEquals(title.center.y,status.center.y,1f);assertTrue(status.left>title.right)
        }
        row("39 · Unmuted")
        compose.onNodeWithContentDescription("Expand volume").performClick()
        assertTrue(AppSettings(store).state.value.volumeExpanded)
        row("39 · Unmuted")
        compose.runOnIdle{state=state.copy(volume=Volume(42,0,""),muted=true)}
        row("42 · Muted")
        compose.onNodeWithContentDescription("Collapse volume").performClick()
        assertFalse(AppSettings(store).state.value.volumeExpanded)
    }
    @Test fun compactGridHasFourEqualColumnsAndSafeUnknownAndDisabledTiles() {
        val sources=Source.known.take(8)+Source("Unknown input",selectable=false)
        var clicked:String?=null
        compose.setContent{ReceiverTheme(true){Box(Modifier.padding(16.dp)){
            SourceTiles(sources,Source("TUNER"),true){clicked=it}
        }}}
        val nodes=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).fetchSemanticsNodes()
        assertEquals(9,nodes.size)
        val first=nodes.first().boundsInRoot
        nodes.take(4).forEach {
            assertEquals(first.top,it.boundsInRoot.top,1f)
            assertEquals(first.width,it.boundsInRoot.width,1f)
            assertEquals(first.height,it.boundsInRoot.height,1f)
        }
        assertTrue(nodes[4].boundsInRoot.top>first.bottom)
        compose.onNodeWithContentDescription("Unknown input").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Tuner").assertIsSelected().performClick()
        assertEquals("TUNER",clicked)
    }
    @Test fun sourcesExpansionPreferenceSurvivesNewSettingsInstance() {
        val store=MemorySettingsStore();var settings by mutableStateOf(AppSettings(store))
        compose.setContent{val prefs by settings.state.collectAsState();ReceiverTheme(true){
            ExpandableSources(listOf(Source("CD")),null,true,prefs.sourcesExpanded,settings::sourcesExpanded){}
        }}
        compose.onNodeWithContentDescription("Expand sources").performClick()
        compose.runOnIdle{settings=AppSettings(store)}
        compose.onNodeWithText("CD").assertIsDisplayed()
        compose.onNodeWithContentDescription("Collapse sources").performClick()
        assertFalse(AppSettings(store).state.value.sourcesExpanded)
    }
    @Test fun countrySearchFiltersLocallyClearsAndKeepsSelectionIdentity() {
        val list=menu();browser(list)
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("pOL")
        compose.onNodeWithText("Poland").assertIsDisplayed()
        compose.onNodeWithText("Argentina").assertDoesNotExist()
        assertEquals(0,calls)
        compose.onNode(hasSetTextAction()).performTextReplacement("no such country")
        compose.onNodeWithText("No results").assertIsDisplayed();assertEquals(0,calls)
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("Argentina").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("pOL")
        compose.onNodeWithText("Poland").performClick()
        assertSame(list.items[11],selected);assertEquals(1,calls)
    }
    @Test fun stationSearchFiltersOnlyLoadedStations() {
        browser(menu(listOf("Jazz Station","Rock Station"),"Stations","Item"))
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithText("Search stations").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("JAZZ")
        compose.onNodeWithText("Jazz Station").assertIsDisplayed()
        compose.onNodeWithText("Rock Station").assertDoesNotExist()
        assertEquals(0,calls)
    }
    @Test fun changingDirectoryResetsSearchAndSingleEntryMenuHidesSearchIcon() {
        var list by mutableStateOf(menu())
        compose.setContent{ReceiverTheme(true){Surface{
            MediaBrowserContent(BrowserState("NET RADIO",BrowserPhase.CONTENT,list),true,
                {calls++},{calls++},{calls++},{calls++},{calls++},{calls++},{calls++},Modifier.fillMaxSize())
        }}}
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Poland")
        compose.runOnIdle{list=menu(listOf("Station"),"Poland","Item")}
        compose.onNodeWithText("Station").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithContentDescription("Search").assertDoesNotExist()
        assertEquals(0,calls)
    }
    @Test fun longRadioMetadataKeepsStopReachable() {
        val station="A very long station name ".repeat(5).trim()
        val song="Long song information ".repeat(8).trim()
        compose.setContent{ReceiverTheme(true){SourceNowPlayingCard("NET RADIO",
            NowPlaying("NET RADIO",station=station,title=song,playbackState="Stop",availability="Ready"),
            null,true,{},{})}}
        compose.onNodeWithText(station).assertIsDisplayed()
        compose.onNodeWithText(song).assertIsDisplayed()
        compose.onNodeWithText("Stopped").assertIsDisplayed()
        compose.onNodeWithText("Stop").assertIsDisplayed().assertIsEnabled()
    }
    @Test fun countryListScrollsNormallyAndRowsAreCompact() {
        browser(menu())
        val first=compose.onNodeWithText("Argentina").fetchSemanticsNode().boundsInRoot
        val second=compose.onNodeWithText("Austria").fetchSemanticsNode().boundsInRoot
        assertTrue(first.height>=48f);assertTrue(second.top-first.top<=56f)
        compose.onNodeWithTag("media_menu").performScrollToIndex(9)
        compose.onNodeWithText("Japan").assertIsDisplayed()
        compose.onNodeWithText("Argentina").assertIsNotDisplayed()
        assertEquals(0,calls)
    }
    @Test fun radioPlayerUsesRealStationMetadataAndOnlyStop() {
        var info by mutableStateOf(NowPlaying("NET RADIO",station="BBC RADIO 1 DANCE",
            artist="Disclosure & Fatoumata Diawara",title="Douha (Mali Mali)",album="Douha (Mali Mali)",
            playbackState="Play",availability="Ready"))
        var action:PlayerAction?=null
        compose.setContent{ReceiverTheme(true){SourceNowPlayingCard("NET RADIO",info,null,true,{action=it},{})}}
        compose.onAllNodesWithText("Douha (Mali Mali)").assertCountEquals(1)
        compose.onNodeWithText("BBC RADIO 1 DANCE").assertIsDisplayed()
        compose.onNodeWithText("Stop").performClick();assertEquals(PlayerAction.STOP,action)
        compose.onNodeWithContentDescription("Next track").assertDoesNotExist()
        compose.runOnIdle{info=info.copy(artist="",title="",album="",playbackState="Stop",availability="Not Ready")}
        compose.onNodeWithText("BBC RADIO 1 DANCE").assertIsDisplayed()
        compose.onNodeWithText("Stop").assertIsNotEnabled()
        compose.onNodeWithText("Douha (Mali Mali)").assertDoesNotExist()
    }
}
