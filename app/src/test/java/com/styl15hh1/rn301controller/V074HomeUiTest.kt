package com.styl15hh1.rn301controller

import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.ui.*
import java.io.File
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="en-rUS-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class V074HomeUiTest {
    @get:Rule val compose=createComposeRule()
    private var value=40
    private var mute=false
    private var source="Spotify"
    private var track="Give Me More"
    private var artist="KSK Mix House"
    private var album="Give Me More"
    private var playback="Play"
    private val commands=mutableListOf<YamahaCommand>()
    private lateinit var repo:YamahaRepository
    private lateinit var vm:ReceiverViewModel
    private fun setup(dark:Boolean=false) {
        val fake=object:YamahaTransport {
            override suspend fun resolve(address:ReceiverAddress)="192.168.1.55"
            override suspend fun description(ip:String)="""<Unit_Description Unit_Name="R-N301"/>"""
            override suspend fun command(ip:String,command:YamahaCommand):String {
                commands+=command
                return when(command) {
                    YamahaCommand.Config->configXml
                    YamahaCommand.Status->statusXml(value=value,mute=if(mute)"On" else "Off",source=source)
                    YamahaCommand.SpotifyInfo->"""<YAMAHA_AV rsp="GET" RC="0"><Spotify><Play_Info><Feature_Availability>Ready</Feature_Availability><Playback_Info>$playback</Playback_Info><Meta_Info><Artist>$artist</Artist><Track>$track</Track><Album>$album</Album></Meta_Info></Play_Info></Spotify></YAMAHA_AV>"""
                    YamahaCommand.TunerInfo->javaClass.getResource("/rn301-tuner-not-ready.xml")!!.readText()
                        .replace("Not Ready","Ready").replace("<Tuned>Negate","<Tuned>Assert")
                        .replace("<Program_Service></Program_Service>","<Program_Service>Radio station</Program_Service>")
                    is YamahaCommand.SetVolume->{value=command.volume.value;ack}
                    is YamahaCommand.Mute->{mute=command.on;ack}
                    is YamahaCommand.SpotifyControl->{playback=command.action.wire;ack}
                    else->ack
                }
            }
        }
        val store=object:AddressStore {override suspend fun read()="";override suspend fun write(address:String)=Unit}
        repo=YamahaRepository(fake,store,io=Dispatchers.Unconfined)
        runBlocking {repo.connect("192.168.1.55");repo.showPlayer(true);repo.showTuner(true);repo.refresh()}
        vm=ReceiverViewModel(repo)
        compose.setContent { ReceiverTheme(dark) {
            ReceiverScreen(vm,vm.settings) {}
        }}
        compose.waitForIdle()
        commands.clear()
    }
    private fun knob()=compose.onNode(hasContentDescription("Rotary volume",substring=true))
    private fun waitForAction() {
        compose.waitUntil(5000) {ShadowLooper.idleMainLooper(200, java.util.concurrent.TimeUnit.MILLISECONDS);!vm.busy.value && !vm.rotaryState.value.pending}
        compose.waitForIdle()
    }
    @Test fun refreshedVolumePlacesValueBesideKnobOnPhone() {
        setup(dark=true)
        compose.onNodeWithContentDescription("Expand volume").performClick()
        val valueBounds=compose.onNodeWithText("40").fetchSemanticsNode().boundsInRoot
        val knobBounds=knob().fetchSemanticsNode().boundsInRoot
        assertTrue(valueBounds.right <= knobBounds.left)
        compose.onNodeWithText("Unmuted",substring=false).assertDoesNotExist()
        compose.onNodeWithText("40 · Unmuted").assertIsDisplayed()
        val headerBounds=compose.onNodeWithContentDescription("Collapse volume").fetchSemanticsNode().boundsInRoot
        val muteBounds=compose.onNodeWithText("Mute").fetchSemanticsNode().boundsInRoot
        assertTrue("Expanded content should fit in 300 dp", muteBounds.bottom-headerBounds.top <= 300f)
        assertTrue(knobBounds.width >= 150f)
        assertTrue(compose.onNodeWithContentDescription("Volume +").fetchSemanticsNode().boundsInRoot.height >= 48f)
        compose.onNodeWithText("Mute").assertIsDisplayed()
        assertTrue(commands.isEmpty())
    }
    @Test @Config(qualifiers="en-rUS-w320dp-h891dp")
    fun narrowVolumeStacksWithoutLosingControls() {
        setup(dark=true)
        compose.onNodeWithContentDescription("Expand volume").performClick()
        val valueBounds=compose.onNodeWithText("40").fetchSemanticsNode().boundsInRoot
        val knobBounds=knob().fetchSemanticsNode().boundsInRoot
        assertTrue(valueBounds.bottom <= knobBounds.top)
        compose.onNodeWithText("Mute").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volume +").performClick();waitForAction()
        assertEquals(41,value)
    }
    private fun capture(name:String) {
        val file=File("build/reports/ui/home-v074-$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun collapsedDefaultExpandsAndCollapsesWithoutAnyReceiverRequest() {
        setup()
        knob().assertDoesNotExist()
        compose.onNodeWithText("40 · Unmuted").assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand volume").assertHasClickAction().performClick()
        knob().assertIsDisplayed()
        compose.onNodeWithContentDescription("Collapse volume").performClick()
        knob().assertDoesNotExist()
        assertTrue(commands.isEmpty())
    }
    @Test fun collapsedSummaryTracksReceiverAndDoesNotAutoExpand() {
        setup()
        value=51;mute=true
        runBlocking {repo.refresh()}
        compose.onNodeWithText("51 · Muted").assertIsDisplayed()
        knob().assertDoesNotExist()
        compose.onNodeWithContentDescription("Expand volume").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"51 · Muted"))
    }
    @Test fun expandedPrecisionAndMuteStillReachReceiverAndReconcile() {
        setup()
        compose.onNodeWithContentDescription("Expand volume").performClick()
        compose.onNodeWithContentDescription("Volume +").performClick();waitForAction()
        assertEquals(41,value)
        compose.onNodeWithContentDescription("Volume −").performClick();waitForAction()
        assertEquals(40,value)
        compose.onNodeWithText("Mute",ignoreCase=false).performClick();waitForAction()
        assertTrue(mute)
        compose.onNodeWithText("Unmute").performClick();waitForAction()
        assertFalse(mute)
        assertEquals(listOf(41,40),commands.filterIsInstance<YamahaCommand.SetVolume>().map{it.volume.value})
        assertEquals(listOf(true,false),commands.filterIsInstance<YamahaCommand.Mute>().map{it.on})
        assertEquals(ConnectionState.CONNECTED,repo.status.value.connectionState)
    }
    @Test fun expandedRotaryGestureReachesExistingLiveWriter() {
        setup()
        compose.onNodeWithContentDescription("Expand volume").performClick()
        knob().performTouchInput {
            fun point(a:Int)=center+Offset(cos(Math.toRadians(a.toDouble())).toFloat(),sin(Math.toRadians(a.toDouble())).toFloat())*(width*.32f)
            down(point(0))
            for(a in 6..60 step 6)moveTo(point(a),20)
            up()
        }
        waitForAction()
        assertEquals(45,value)
        assertEquals(45,repo.status.value.volume?.value)
        assertTrue(commands.any{it is YamahaCommand.SetVolume && it.volume.value==45})
    }
    @Test fun collapsingDuringGestureCancelsCaptureWithoutDeferredWrites() {
        setup()
        compose.onNodeWithContentDescription("Expand volume").performClick()
        knob().performTouchInput {
            down(center+Offset(width*.32f,0f))
            moveTo(center+Offset(width*.16f,width*.277f),20)
        }
        compose.onNodeWithContentDescription("Collapse volume").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.onRoot().performTouchInput { cancel() }
        waitForAction()
        assertFalse(vm.rotaryState.value.active)
        val writes=commands.filterIsInstance<YamahaCommand.SetVolume>().size
        compose.runOnIdle{ShadowLooper.idleMainLooper(500,java.util.concurrent.TimeUnit.MILLISECONDS)}
        assertEquals(writes,commands.filterIsInstance<YamahaCommand.SetVolume>().size)
        knob().assertDoesNotExist()
    }
    @Test fun freshHomeRetainsSavedExpandedVolume() {
        setup()
        compose.onNodeWithContentDescription("Expand volume").performClick()
        compose.runOnIdle{vm.settings()}
        compose.runOnIdle{vm.home()}
        compose.onNodeWithContentDescription("Collapse volume").assertIsDisplayed()
        knob().assertIsDisplayed()
    }
    @Test fun unknownMuteRemainsExplicitInCollapsedSummary() {
        compose.setContent{MaterialTheme{ExpandableVolume(ReceiverStatus(volume=Volume(40,0,"")), false, {}) {}}}
        compose.onNodeWithText("40 · Mute unavailable").assertIsDisplayed()
    }
    @Test fun physicalSpotifyMetadataIsDeduplicatedAndNavigationExplicit() {
        setup()
        compose.onNodeWithTag("spotify_now_playing_artwork").assertIsDisplayed()
        compose.onAllNodesWithText("Give Me More").assertCountEquals(1)
        compose.onNodeWithText("KSK Mix House").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
        compose.onNodeWithText("Stop").assertDoesNotExist()
        compose.onNodeWithContentDescription("Open Now Playing").performClick()
        compose.onNodeWithText("Now Playing").assertIsDisplayed()
        compose.onNodeWithText("Stop").assertDoesNotExist()
    }
    @Test fun pausedUsesPlayAndCommandsFollowReadback() {
        playback="Pause";setup()
        compose.onNodeWithContentDescription("Play").performClick();waitForAction()
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
        assertTrue(commands.contains(YamahaCommand.SpotifyControl(PlayerAction.PLAY)))
        compose.onNodeWithContentDescription("Pause").performClick();waitForAction()
        compose.onNodeWithContentDescription("Play").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous track").performClick();waitForAction()
        compose.onNodeWithContentDescription("Next track").performClick();waitForAction()
        assertTrue(commands.contains(YamahaCommand.SpotifyControl(PlayerAction.PREVIOUS)))
        assertTrue(commands.contains(YamahaCommand.SpotifyControl(PlayerAction.NEXT)))
    }
    @Test fun uniqueAlbumAndTrackArtistRemainVisible() {
        track="Money for Nothing";artist="Dire Straits";album="Brothers in Arms";setup()
        listOf(track,artist,album).forEach{compose.onNodeWithText(it).assertIsDisplayed()}
    }
    @Test fun missingMetadataAndUnknownStateDoNotInventPlayback() {
        track="";artist="";album="";playback="Unexpected";setup()
        compose.onNodeWithContentDescription("Play").assertDoesNotExist()
        compose.onNodeWithContentDescription("Pause").assertDoesNotExist()
        compose.onNodeWithContentDescription("Playback state unavailable").assertIsNotEnabled()
    }
    @Test fun lightSpotifyCollapsedRender() {setup();capture("spotify-collapsed-light")}
    @Test fun darkSpotifyCollapsedRender() {setup(true);capture("spotify-collapsed-dark")}
    @Test fun expandedSpotifyRender() {
        setup();compose.onNodeWithContentDescription("Expand volume").performClick()
        knob().assertIsDisplayed();capture("spotify-expanded-light")
    }
    @Test fun tunerCollapsedRenderPreservesStation() {
        source="TUNER";setup()
        compose.onNodeWithText("Radio station").assertIsDisplayed()
        knob().assertDoesNotExist();capture("tuner-collapsed-light")
    }
    @Test fun longMetadataRenderStaysWithinViewport() {
        track="A very long track title that needs more than one line on a phone screen"
        artist="An artist with a deliberately long descriptive name"
        album="An album with a different and equally long name";setup(true)
        listOf(track,artist,album).forEach{compose.onNodeWithText(it).assertIsDisplayed()}
        compose.onNodeWithContentDescription("Previous track").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next track").assertIsDisplayed()
        val previous=compose.onNodeWithContentDescription("Previous track").fetchSemanticsNode().boundsInRoot.center
        val pause=compose.onNodeWithContentDescription("Pause").fetchSemanticsNode().boundsInRoot.center
        val next=compose.onNodeWithContentDescription("Next track").fetchSemanticsNode().boundsInRoot.center
        assertEquals(pause.x-previous.x,next.x-pause.x,1f)
        assertEquals(previous.y,pause.y,1f);assertEquals(next.y,pause.y,1f)
        compose.onNodeWithContentDescription("Expand sources").assertIsDisplayed()
        capture("spotify-long-dark")
    }
    companion object {private const val ack="""<YAMAHA_AV rsp="PUT" RC="0"/>"""}
}
