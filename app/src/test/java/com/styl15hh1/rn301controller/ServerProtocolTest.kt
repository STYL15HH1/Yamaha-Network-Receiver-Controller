package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import org.junit.Assert.*
import org.junit.Test

// Synthetic fixtures derived from A's parser and D's eight-line receiver controller, not live captures.
internal fun mediaListXml(layer: Int = 1, current: Int = 1, max: Int = 1,
    entries: String = "<Line_1><Txt>NAS</Txt><Attribute>Container</Attribute></Line_1>",
    status: String = "Ready", title: String = "Servers") =
    """<YAMAHA_AV rsp="GET" RC="0"><SERVER><List_Info><Menu_Status>$status</Menu_Status>
    <Menu_Layer>$layer</Menu_Layer><Menu_Name>$title</Menu_Name>
    <Cursor_Position><Current_Line>$current</Current_Line><Max_Line>$max</Max_Line></Cursor_Position>
    <Current_List>$entries</Current_List></List_Info></SERVER></YAMAHA_AV>"""
internal fun serverInfoXml(state: String = "Play", metadata: String = "<Artist>Artist</Artist><Album>Album</Album><Song>Track</Song>") =
    """<YAMAHA_AV rsp="GET" RC="0"><SERVER><Play_Info><Feature_Availability>Ready</Feature_Availability>
    <Playback_Info>$state</Playback_Info><Meta_Info>$metadata</Meta_Info></Play_Info></SERVER></YAMAHA_AV>"""

class ServerProtocolTest {
    private val parser = YamahaXmlParser()
    private fun body(command: YamahaCommand) = YamahaXmlBuilder.build(command).substringAfter("?>")
    @Test fun listGetUsesServerScope() {
        assertEquals("""<YAMAHA_AV cmd="GET"><SERVER><List_Info>GetParam</List_Info></SERVER></YAMAHA_AV>""", body(YamahaCommand.ServerList))
    }
    @Test fun directoryAndTrackUsePageRelativeDirectSelection() {
        for (line in listOf(1,8)) assertEquals("""<YAMAHA_AV cmd="PUT"><SERVER><List_Control><Direct_Sel>Line_$line</Direct_Sel></List_Control></SERVER></YAMAHA_AV>""",
            body(YamahaCommand.ServerSelect(line)))
        assertThrows(IllegalArgumentException::class.java) { body(YamahaCommand.ServerSelect(9)) }
        assertThrows(IllegalArgumentException::class.java) { body(YamahaCommand.ServerSelect(0)) }
    }
    @Test fun backAndPageCommands() {
        assertEquals("""<YAMAHA_AV cmd="PUT"><SERVER><List_Control><Cursor>Return</Cursor></List_Control></SERVER></YAMAHA_AV>""", body(YamahaCommand.ServerBack))
        for (next in listOf(true,false)) assertEquals("""<YAMAHA_AV cmd="PUT"><SERVER><List_Control><Page>${if(next) "Down" else "Up"}</Page></List_Control></SERVER></YAMAHA_AV>""", body(YamahaCommand.ServerPage(next)))
    }
    @Test fun fivePlaybackCommandsUseServerNotSpotifyOrPlayer() {
        val values = mapOf(PlayerAction.PLAY to "Play", PlayerAction.PAUSE to "Pause", PlayerAction.STOP to "Stop",
            PlayerAction.NEXT to "Skip Fwd", PlayerAction.PREVIOUS to "Skip Rev")
        values.forEach { (action,value) -> assertEquals("""<YAMAHA_AV cmd="PUT"><SERVER><Play_Control><Playback>$value</Playback></Play_Control></SERVER></YAMAHA_AV>""", body(YamahaCommand.ServerControl(action))) }
        assertEquals("""<YAMAHA_AV cmd="GET"><SERVER><Play_Info>GetParam</Play_Info></SERVER></YAMAHA_AV>""", body(YamahaCommand.ServerInfo))
    }
    @Test fun singleContainerAndMenuPosition() {
        val list = parser.serverList(mediaListXml())
        assertEquals("SERVER", list.source); assertEquals("Servers", list.title)
        assertEquals(1,list.layer); assertEquals(1,list.currentLine); assertTrue(list.root)
        assertTrue(list.items.single().browsable); assertFalse(list.items.single().playable)
        assertEquals(MediaType.DIRECTORY,list.items.single().type)
    }
    @Test fun emptyRootIsAValidReadyList() {
        val list = parser.serverList(mediaListXml(current=0,max=0,entries=""))
        assertTrue(list.ready); assertTrue(list.items.isEmpty()); assertFalse(list.next); assertFalse(list.previous)
    }
    @Test fun multipleItemsDecodeEntitiesAndAttributes() {
        val list = parser.serverList(mediaListXml(max=3,entries="""
            <Line_1><Txt>Rock &amp; Roll</Txt><Attribute>Container</Attribute></Line_1>
            <Line_2><Txt>Song</Txt><Attribute>Item</Attribute></Line_2>
            <Line_3><Txt>Status</Txt><Attribute>Unselectable</Attribute></Line_3>"""))
        assertEquals(3,list.items.size); assertEquals("Rock & Roll",list.items[0].title)
        assertEquals(MediaType.TRACK,list.items[1].type); assertTrue(list.items[1].playable)
        assertEquals(MediaType.UNKNOWN,list.items[2].type); assertFalse(list.items[2].selectable)
    }
    @Test fun albumTitleAndDeepContainerDoNotInventAlbumType() {
        val list = parser.serverList(mediaListXml(layer=6,entries="<Line_1><Txt>Albums</Txt><Attribute>Container</Attribute></Line_1>"))
        assertEquals(MediaType.DIRECTORY,list.items.single().type)
    }
    @Test fun unknownItemMetadataStaysSafe() {
        val list = parser.serverList(mediaListXml(entries="<Line_1><Txt>New type</Txt><Attribute>Future</Attribute></Line_1>"))
        assertFalse(list.items.single().selectable)
    }
    @Test fun eightSlotPaginationIncludesPartialLastWindow() {
        val middle = parser.serverList(mediaListXml(current=9,max=18))
        assertEquals(2,middle.page); assertEquals(3,middle.pages); assertTrue(middle.previous); assertTrue(middle.next)
        val last = parser.serverList(mediaListXml(current=18,max=18))
        assertEquals(3,last.page); assertFalse(last.next)
    }
    @Test fun busyCanOmitListAndPositions() {
        val xml = """<YAMAHA_AV rsp="GET" RC="0"><SERVER><List_Info><Menu_Status>Busy</Menu_Status></List_Info></SERVER></YAMAHA_AV>"""
        assertFalse(parser.serverList(xml).ready)
    }
    @Test fun malformedWrongSourceAndInvalidPositionsRejected() {
        listOf("<broken",mediaListXml().replace("SERVER","NET_RADIO"),
            mediaListXml(current=4,max=1),mediaListXml().replace("Line_1","Line_9"),
            mediaListXml().replace("<Menu_Layer>1</Menu_Layer>","")).forEach {
            assertThrows(YamahaException::class.java) { parser.serverList(it) }
        }
    }
    @Test fun metadataUsesReceiverFieldsAndPlayback() {
        val info = parser.server(serverInfoXml())
        assertEquals("SERVER",info.source); assertEquals("Track",info.title); assertEquals("Artist",info.artist)
        assertEquals("Album",info.album); assertEquals(PlaybackState.PLAYING,info.playback)
        assertEquals(PlaybackState.PAUSED,parser.server(serverInfoXml("Pause")).playback)
        assertEquals(PlaybackState.STOPPED,parser.server(serverInfoXml("Stop")).playback)
    }
    @Test fun missingMetadataAndUnknownPlaybackAreSafe() {
        val info = parser.server(serverInfoXml("Future",""))
        assertNull(info.title); assertNull(info.artist); assertNull(info.album); assertEquals(PlaybackState.UNKNOWN,info.playback)
    }
    @Test fun malformedMetadataRejected() {
        assertThrows(YamahaException::class.java) { parser.server("<SERVER>") }
        assertThrows(YamahaException::class.java) { parser.server(serverInfoXml().replace("SERVER","Spotify")) }
    }
}
