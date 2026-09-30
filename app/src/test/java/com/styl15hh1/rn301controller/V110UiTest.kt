package com.styl15hh1.rn301controller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="en-rUS-w411dp-h891dp")
class V110UiTest {
    @get:Rule val compose=createComposeRule()
    private val entries=(1..17).map { MediaItem((it-1)%8+1,"Track $it","Item","SERVER",(it-1)/8+1) }
    private var back=0;private var root=0;private var refresh=0;private var receiver=0;private var player=0
    private var selected:MediaItem?=null
    private fun render(enabled:Boolean=true) {
        val list=MediaList("SERVER","Ready",3,"Album",1,17,entries,true)
        compose.setContent { MaterialTheme { Surface {
            MediaBrowserContent(BrowserState("SERVER",BrowserPhase.CONTENT,list),enabled,
                {back++},{root++},{refresh++},{selected=it},{receiver++},{player++},Modifier.fillMaxSize())
        } } }
    }
    @Test fun serverHidesTechnicalPagesAndScrollsAcrossAggregate() {
        render()
        for(text in listOf("Menu level","Page ","Previous page","Next page"))
            compose.onNodeWithText(text,substring=true).assertDoesNotExist()
        compose.onNodeWithTag("media_menu").performScrollToIndex(16)
        compose.onNodeWithText("Track 17").performClick()
        assertSame(entries[16],selected)
    }
    @Test fun serverBackRootRefreshAndExistingPlayerNavigationDispatchSeparately() {
        render()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Server root").performClick()
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.onNodeWithText("Receiver").performClick()
        compose.onNodeWithText("Now Playing").performClick()
        assertEquals(listOf(1,1,1,1,1),listOf(back,root,refresh,receiver,player))
        compose.onNodeWithContentDescription("App Home").assertDoesNotExist()
    }
    @Test fun serverSearchIsLocalClearableAndKeepsOriginalTrackIdentity() {
        render()
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("track 17")
        compose.onNodeWithText("Track 17").assertIsDisplayed()
        compose.onNodeWithText("Track 1").assertDoesNotExist()
        assertEquals(0,back+root+refresh+receiver+player);assertNull(selected)
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("Track 1").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("TRACK 17")
        compose.onNodeWithText("Track 17").performClick()
        assertSame(entries[16],selected)
    }
    @Test fun disabledServerControlsCannotNavigateOrSelect() {
        render(false)
        for(label in listOf("Back","Server root","Refresh","Search"))
            compose.onNodeWithContentDescription(label).assertIsNotEnabled()
        compose.onNodeWithText("Track 1").performClick()
        assertNull(selected)
    }
    @Test fun appHomeIsShownOnlyAwayFromReceiverAndDistinctFromBrowserRoot() {
        var atHome by mutableStateOf(false);var homes=0;var roots=0
        val state=ReceiverStatus(connectionState=ConnectionState.CONNECTED,powerState=PowerState.ON)
        compose.setContent { MaterialTheme { Column {
            ReceiverHeader(state,false,{},{},{},home=if(atHome)null else {{homes++;atHome=true}})
            MediaBrowserContent(BrowserState("NET RADIO"),true,{},{roots++},{},{},{},{},Modifier.weight(1f))
        } } }
        compose.onNodeWithContentDescription("App Home").assertIsDisplayed()
        compose.onNodeWithContentDescription("Radio root").performClick()
        assertEquals(1,roots);assertEquals(0,homes)
        compose.onNodeWithContentDescription("App Home").performClick()
        assertEquals(1,homes);assertEquals(1,roots)
        compose.onNodeWithContentDescription("App Home").assertDoesNotExist()
    }
}
