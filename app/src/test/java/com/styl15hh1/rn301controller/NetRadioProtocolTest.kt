package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.player.PlayerSources
import com.styl15hh1.rn301controller.ui.*
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

// Synthetic fixtures using the R-N301 integration and receiver-served JS grammar.
internal fun radioListXml(layer: Int = 1, current: Int = 1, max: Int = 1,
    entries: String = "<Line_1><Txt>Locations</Txt><Attribute>Container</Attribute></Line_1>",
    status: String = "Ready", title: String = "Internet Radio") =
    mediaListXml(layer,current,max,entries,status,title).replace("SERVER","NET_RADIO")
internal fun radioInfoXml(state: String = "Play", metadata: String = "<Station>Radio</Station><Song>Programme</Song>") =
    serverInfoXml(state,metadata).replace("SERVER","NET_RADIO")

class NetRadioProtocolTest {
    private val parser=YamahaXmlParser()
    private fun body(c:YamahaCommand)=YamahaXmlBuilder.build(c).substringAfter("?>")
    @Test fun listAndInfoCommandsUseNetRadioScope() {
        assertEquals("""<YAMAHA_AV cmd="GET"><NET_RADIO><List_Info>GetParam</List_Info></NET_RADIO></YAMAHA_AV>""",body(YamahaCommand.NetRadioList))
        assertEquals("""<YAMAHA_AV cmd="GET"><NET_RADIO><Play_Info>GetParam</Play_Info></NET_RADIO></YAMAHA_AV>""",body(YamahaCommand.NetRadioInfo))
    }
    @Test fun selectBackAndPagingMatchReceiverController() {
        for(line in listOf(1,8)) assertEquals("""<YAMAHA_AV cmd="PUT"><NET_RADIO><List_Control><Direct_Sel>Line_$line</Direct_Sel></List_Control></NET_RADIO></YAMAHA_AV>""",body(YamahaCommand.NetRadioSelect(line)))
        assertThrows(IllegalArgumentException::class.java){body(YamahaCommand.NetRadioSelect(9))}
        assertEquals("""<YAMAHA_AV cmd="PUT"><NET_RADIO><List_Control><Cursor>Return</Cursor></List_Control></NET_RADIO></YAMAHA_AV>""",body(YamahaCommand.NetRadioBack))
        for(next in listOf(true,false)) assertEquals("""<YAMAHA_AV cmd="PUT"><NET_RADIO><List_Control><Page>${if(next) "Down" else "Up"}</Page></List_Control></NET_RADIO></YAMAHA_AV>""",body(YamahaCommand.NetRadioPage(next)))
    }
    @Test fun onlyVerifiedPlaybackCommandsCanBeBuilt() {
        for(action in listOf(PlayerAction.PLAY,PlayerAction.STOP)) assertEquals("""<YAMAHA_AV cmd="PUT"><NET_RADIO><Play_Control><Playback>${action.wire}</Playback></Play_Control></NET_RADIO></YAMAHA_AV>""",body(YamahaCommand.NetRadioControl(action)))
        for(action in listOf(PlayerAction.PAUSE,PlayerAction.NEXT,PlayerAction.PREVIOUS))
            assertThrows(IllegalArgumentException::class.java){body(YamahaCommand.NetRadioControl(action))}
        assertEquals(setOf(PlayerAction.STOP),PlayerSources.controls("NET RADIO"))
        assertEquals(PlayerAction.entries.toSet(),PlayerSources.controls("SERVER"))
    }
    @Test fun rootSingleDirectoryAndPosition() {
        val list=parser.netRadioList(radioListXml())
        assertEquals("NET RADIO",list.source);assertTrue(list.root);assertTrue(list.ready)
        assertEquals(1,list.layer);assertEquals(1,list.currentLine);assertEquals(1,list.maxLine)
        assertEquals(MediaType.DIRECTORY,list.items.single().type)
    }
    @Test fun emptyDirectoryIsNotAnError() {
        val list=parser.netRadioList(radioListXml(current=0,max=0,entries=""))
        assertTrue(list.ready);assertTrue(list.items.isEmpty());assertFalse(list.next)
    }
    @Test fun mixedItemsEntitiesUnicodeAndUnknownTypes() {
        val list=parser.netRadioList(radioListXml(max=4,entries="""
        <Line_1><Txt>Polska &amp; Europa</Txt><Attribute>Container</Attribute></Line_1>
        <Line_2><Txt>日本</Txt><Attribute>Container</Attribute></Line_2>
        <Line_3><Txt>Radio</Txt><Attribute>Item</Attribute></Line_3>
        <Line_4><Txt>Unavailable</Txt><Attribute>Unselectable</Attribute></Line_4>"""))
        assertEquals(listOf(MediaType.DIRECTORY,MediaType.DIRECTORY,MediaType.STATION,MediaType.UNKNOWN),list.items.map{it.type})
        assertEquals("Polska & Europa",list.items.first().title)
        assertFalse(list.items.last().selectable)
    }
    @Test fun loadingAndBusyMayOmitOptionalPositions() {
        for(status in listOf("Busy","Loading")) {
            val xml="""<YAMAHA_AV rsp="GET" RC="0"><NET_RADIO><List_Info><Menu_Status>$status</Menu_Status></List_Info></NET_RADIO></YAMAHA_AV>"""
            val list=parser.netRadioList(xml)
            assertFalse(list.ready);assertNull(list.layer);assertTrue(list.items.isEmpty())
        }
    }
    @Test fun eightSlotPagesAndLastPage() {
        val list=parser.netRadioList(radioListXml(layer=5,current=17,max=18))
        assertEquals(5,list.layer);assertEquals(3,list.page);assertEquals(3,list.pages)
        assertTrue(list.previous);assertFalse(list.next)
    }
    @Test fun malformedAndWrongScopeRejected() {
        for(xml in listOf("<bad",mediaListXml(),radioListXml(current=30,max=2),radioListXml(entries="<Line_9><Txt>X</Txt></Line_9>")))
            assertThrows(YamahaException::class.java){parser.netRadioList(xml)}
    }
    @Test fun stationAndProgrammeMapWithoutInventingFields() {
        val info=parser.netRadio(radioInfoXml(metadata="<Station>Radio 357</Station><Title>Audycja</Title><Artist>Host</Artist>"))
        assertEquals("NET RADIO",info.source);assertEquals("Radio 357",info.station)
        assertEquals("Audycja",info.title);assertEquals("Host",info.artist);assertNull(info.album)
        assertEquals(PlaybackState.PLAYING,info.playback)
    }
    @Test fun missingMetadataAndUnknownPlaybackRemainSafe() {
        val info=parser.netRadio(radioInfoXml("Future",""))
        assertNull(info.station);assertNull(info.title);assertEquals(PlaybackState.UNKNOWN,info.playback)
        assertEquals(PlaybackState.STOPPED,parser.netRadio(radioInfoXml("Stop")).playback)
        assertThrows(YamahaException::class.java){parser.netRadio("<broken")}
    }
    @Test fun artworkDisplayAndDisabledConfiguration() {
        assertEquals(0.82f,SourceArtwork.spotifyScale,0f)
        assertFalse(SourceArtwork.showLabel("Spotify"));assertTrue(SourceArtwork.showLabel("AirPlay"))
        assertEquals(0.38f,ControlPolicy.contentAlpha(false),0f)
        val image=ImageIO.read(File("src/main/res/drawable-nodpi/airplay_artwork.png"))
        assertEquals(409,image.width);assertEquals(358,image.height);assertTrue(image.colorModel.hasAlpha())
    }
    @Test fun verifiedNativeVolumeAndDisconnectedPoliciesArePreserved() {
        val state=ReceiverStatus(connectionState=ConnectionState.CONNECTED,powerState=PowerState.ON,volume=Volume(40,0,""))
        assertTrue(ControlPolicy.powered(state,false));assertNull(ControlPolicy.sliderRange(state))
        assertEquals(41,NativeVolumeStep.target(state.volume!!,1)!!.value)
        assertFalse(ControlPolicy.powered(state.copy(connectionState=ConnectionState.UNAVAILABLE),false))
    }
}
