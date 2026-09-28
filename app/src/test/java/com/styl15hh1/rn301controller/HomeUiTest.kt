package com.styl15hh1.rn301controller

import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.ui.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="en-rUS-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeUiTest {
    @get:Rule val compose=createComposeRule()
    private fun render(dark:Boolean) {
        val fake=object:YamahaTransport {
            override suspend fun resolve(address:ReceiverAddress)="192.168.1.55"
            override suspend fun description(ip:String)="""<Unit_Description Unit_Name="R-N301"/>"""
            override suspend fun command(ip:String,command:YamahaCommand)=when(command) {
                YamahaCommand.Config->configXml
                YamahaCommand.Status->statusXml(value=40,source="Spotify")
                YamahaCommand.SpotifyInfo->"""<YAMAHA_AV rsp="GET" RC="0"><Spotify><Play_Info><Playback_Info>Play</Playback_Info><Meta_Info><Artist>Test artist</Artist><Track>Test track</Track><Album>Test album</Album></Meta_Info></Play_Info></Spotify></YAMAHA_AV>"""
                else->"""<YAMAHA_AV rsp="PUT" RC="0"/>"""
            }
        }
        val store=object:AddressStore {override suspend fun read()="";override suspend fun write(address:String)=Unit}
        val repo=YamahaRepository(fake,store)
        runBlocking { repo.connect("192.168.1.55");repo.showPlayer(true);repo.refresh() }
        assertNull(repo.status.value.error)
        compose.setContent {
            val vm=remember { ReceiverViewModel(repo) }
            MaterialTheme(colorScheme=if(dark) darkColorScheme() else lightColorScheme()) {
                ReceiverScreen(vm,vm.settings) {}
            }
        }
        compose.onNodeWithText("R-N301").assertIsDisplayed()
        compose.onNodeWithText("Connected",substring=true).assertIsDisplayed()
        compose.onNodeWithText("Favorites").assertIsDisplayed()
        val heading=compose.onNodeWithText("Favorites")
        val textLayouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        heading.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(textLayouts) }
        val layout=textLayouts.single()
        assertEquals(layout.size.width / 2f,(layout.getLineLeft(0)+layout.getLineRight(0))/2f,1f)
        val headingBounds=heading.fetchSemanticsNode().boundsInRoot
        assertEquals(compose.onRoot().fetchSemanticsNode().boundsInRoot.center.x,headingBounds.center.x,1f)
        compose.onNodeWithText("Test track").assertIsDisplayed()
        compose.onNodeWithText("CD").assertDoesNotExist()
        val favoritesY=compose.onNodeWithText("Favorites").fetchSemanticsNode().boundsInRoot.top
        val trackY=compose.onNodeWithText("Test track").fetchSemanticsNode().boundsInRoot.top
        val sourcesY=compose.onNodeWithContentDescription("Expand sources").fetchSemanticsNode().boundsInRoot.top
        assertTrue(favoritesY<trackY && trackY<sourcesY)
        val tuner=compose.onNodeWithText("Tuner").fetchSemanticsNode().boundsInRoot
        val optical=compose.onNodeWithText("Optical").fetchSemanticsNode().boundsInRoot
        assertEquals(optical.center.y,tuner.center.y,1f)
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        val output=File("build/reports/ui/home-v074-hierarchy-${if(dark) "dark" else "light"}.png")
        requireNotNull(output.parentFile).mkdirs()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun lightHomePrioritizesVolumeFavoritesAndPlayingBeforeSources()=render(false)
    @Test fun darkHomePreservesSameHierarchyAndBranding()=render(true)
}
