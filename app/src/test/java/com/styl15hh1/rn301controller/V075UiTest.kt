package com.styl15hh1.rn301controller

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.ui.*
import java.io.File
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
class V075UiTest {
    @get:Rule val compose=createComposeRule()
    private val favorites=listOf("OPTICAL","TUNER","Spotify","NET RADIO").map{Source(it)}
    private var back=0;private var home=0;private var refresh=0;private var player=0
    private var selected:MediaItem?=null
    private val entries=(listOf("Japan","Mexico","Myanmar","Peru","Poland","Portugal")+
        (1..20).map{"Country $it"}+"Unmapped country").mapIndexed { index,name ->
            MediaItem(index%8+1,name,"Container","NET RADIO",index/8+1)
        }
    private fun render(dark:Boolean=false, enabled:Boolean=true) {
        val list=MediaList("NET RADIO","Ready",2,"Countries",1,entries.size,entries,true)
        compose.setContent{MaterialTheme(colorScheme=if(dark)darkColorScheme() else lightColorScheme()){
            Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize()){
                Box(Modifier.padding(horizontal=16.dp)){QuickSourceRow(favorites,Source("NET RADIO"),true){}}
                MediaBrowserContent(BrowserState("NET RADIO",BrowserPhase.CONTENT,list),enabled,
                    {back++},{home++},{refresh++},{selected=it},
                    {},{player++},Modifier.weight(1f))
            }}
        }}
    }
    private fun capture(name:String) {
        val file=File("build/reports/ui/v075-$name.png");file.parentFile!!.mkdirs()
        file.outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun oneToFourFavoritesStayInOneRowAndFourHaveEqualWidths() {
        var count by mutableIntStateOf(1)
        var selected:String?=null
        compose.setContent{MaterialTheme{Box(Modifier.padding(horizontal=16.dp)){QuickSourceRow(favorites.take(count),null,true){selected=it}}}}
        for(size in 1..4) {
            compose.runOnIdle{count=size}
            val nodes=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).fetchSemanticsNodes()
            assertEquals(size,nodes.size)
            val first=nodes.first().boundsInRoot
            for(node in nodes) {
                assertEquals(first.center.y,node.boundsInRoot.center.y,1f)
                assertEquals(first.width,node.boundsInRoot.width,1f)
                assertTrue(node.boundsInRoot.height>=68f)
            }
        }
        compose.onNodeWithText("Net Radio").performClick()
        assertEquals("NET RADIO",selected)
    }
    @Test fun toolbarActionsAndNowPlayingRemainAccessible() {
        render()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Radio root").performClick()
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.onNodeWithText("Now Playing").performClick()
        assertEquals(listOf(1,1,1,1),listOf(back,home,refresh,player))
    }
    @Test fun radioHidesDepthAndPaginationAndScrollsToUnknownCountry() {
        render()
        compose.onAllNodesWithText("Net Radio").assertCountEquals(2) // favorite + compact toolbar
        compose.onNodeWithText("Menu level",substring=true).assertDoesNotExist()
        compose.onNodeWithText("Page ",substring=true).assertDoesNotExist()
        compose.onNodeWithText("Previous page").assertDoesNotExist()
        compose.onNodeWithText("Next page").assertDoesNotExist()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Unmapped country"))
        compose.onNodeWithText("Unmapped country").performClick()
        assertEquals("Unmapped country",selected?.title)
    }
    @Test fun disabledNavigationAndRowsCannotDispatch() {
        render(enabled=false)
        for(label in listOf("Back","Radio root","Refresh"))
            compose.onNodeWithContentDescription(label).assertIsNotEnabled()
        compose.onNodeWithText("Japan").performClick()
        assertNull(selected)
    }
    @Test fun lightCountriesAndFourFavoritesRender() {render();capture("radio-light")}
    @Test fun darkCountriesAndFourFavoritesRender() {render(true);capture("radio-dark")}
}
