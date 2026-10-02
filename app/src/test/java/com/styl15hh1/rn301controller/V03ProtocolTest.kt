package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import org.junit.Assert.*
import org.junit.Test

class V03ProtocolTest {
    private val parser = YamahaXmlParser()
    private fun fixture(name: String) = javaClass.getResource("/$name")!!.readText()
    private fun spotify(body: String) = "<YAMAHA_AV rsp=\"GET\" RC=\"0\"><Spotify><Play_Info>$body</Play_Info></Spotify></YAMAHA_AV>"
    private fun xml(command: YamahaCommand) = YamahaXmlBuilder.build(command).substringAfter("?>")

    @Test fun nativeVolumeStepPreservesReceiverEncoding() {
        assertEquals(Volume(41, 0, ""), NativeVolumeStep.target(Volume(40, 0, ""), 1))
        assertEquals(Volume(39, 0, ""), NativeVolumeStep.target(Volume(40, 0, ""), -1))
        assertEquals("40 (native)", Volume(40, 0, "").display)
    }
    @Test fun nativeVolumeDoesNotGuessDbOrMax() {
        assertNull(NativeVolumeStep.target(Volume(-400, 1, "dB"), 1))
        assertNull(NativeVolumeStep.target(Volume(99, 0, ""), 1))
        assertNull(NativeVolumeStep.target(Volume(0, 0, ""), -1))
        assertNull(NativeVolumeStep.target(Volume(40, 0, ""), 2))
    }
    @Test fun nativeStepGeneratesVerifiedAbsoluteXmlAndAck() {
        val target = NativeVolumeStep.target(Volume(40, 0, ""), 1)!!
        assertEquals("<YAMAHA_AV cmd=\"PUT\"><Main_Zone><Volume><Lvl><Val>41</Val><Exp>0</Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>",
            xml(YamahaCommand.SetVolume(target)))
        parser.response("<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>", "PUT")
    }
    @Test fun physicalFmConfigUsesFiveHundredthsNotTenth() {
        val range = parser.tunerConfig(fixture("rn301-tuner-config.xml"))!!
        assertEquals(FmRange(8750, 10800, 5), range)
        assertEquals(9745, range.stepFrom(ReceiverFrequency(9740, 2, "MHz"), 1))
        assertEquals(9735, range.stepFrom(ReceiverFrequency(9740, 2, "MHz"), -1))
    }
    @Test fun fmInputUsesExactAdvertisedGrid() {
        val range = FmRange(8750, 10800, 5)
        assertEquals(10165, range.parse("101.65"))
        assertEquals(10165, range.parse("101,65"))
        assertNull(range.parse("101.63"))
        assertNull(range.parse("101.6501"))
        assertNull(range.parse("108.05"))
        assertNull(range.parse("NaN"))
        assertNull(range.stepFrom(ReceiverFrequency(10800, 2, "MHz"), 1))
    }
    @Test fun otherRegionalStepIsRespected() {
        val range = FmRange(8750, 10800, 10)
        assertNull(range.parse("101.65"))
        assertEquals(10170, range.stepFrom(ReceiverFrequency(10160, 2, "MHz"), 1))
    }
    @Test fun malformedFmConfigIsRejected() {
        val text = fixture("rn301-tuner-config.xml").replace("<Val>5</Val>", "<Val>0</Val>")
        assertThrows(YamahaException::class.java) { parser.tunerConfig(text) }
        assertNull(parser.tunerConfig("<YAMAHA_AV rsp=\"GET\" RC=\"0\"><Tuner><Config/></Tuner></YAMAHA_AV>"))
    }
    @Test fun exactDirectFmCommand() {
        assertEquals("<YAMAHA_AV cmd=\"PUT\"><Tuner><Play_Control><Tuning><Band>FM</Band><Freq><FM><Val>10165</Val><Exp>2</Exp><Unit>MHz</Unit></FM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>",
            xml(YamahaCommand.TuneFm(10165)))
        assertTrue(xml(YamahaCommand.TunerConfig).contains("<Tuner><Config>GetParam</Config></Tuner>"))
    }
    @Test fun exactSeekCommandsFromReceiverController() {
        for (up in listOf(true, false)) assertEquals(
            "<YAMAHA_AV cmd=\"PUT\"><Tuner><Play_Control><Tuning><Freq><FM><Val>Auto ${if (up) "Up" else "Down"}</Val></FM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>",
            xml(YamahaCommand.SeekFm(up)))
    }
    @Test fun seekTransientIsNotMalformedFrequency() {
        val info = parser.tuner(fixture("rn301-tuner-not-ready.xml").replace("<Val>9740</Val>", "<Val>Auto Up</Val>"))
        assertNull(info.frequency)
        assertEquals("Auto Up", info.tuningState)
    }
    @Test fun presetFrequencyIsNotInventedStationName() {
        val view = TunerPreset(2, "FM 97.90 MHz").presentation(null)
        assertNull(view.station)
        assertEquals("97.90 MHz", view.frequency)
        assertEquals(2, view.number)
        assertFalse(view.selected)
    }
    @Test fun rdsOnlyLabelsCurrentPreset() {
        val current = TunerStatus(preset = 1, nowPlaying = NowPlaying("TUNER", station = "Radio 357"))
        assertEquals("Radio 357", TunerPreset(1, "FM 101.20 MHz").presentation(current).station)
        assertNull(TunerPreset(2, "FM 97.90 MHz").presentation(current).station)
        assertTrue(TunerPreset(1, "FM 101.20 MHz").presentation(current).selected)
    }
    @Test fun namedPresetRetainsNameWithoutFakeFrequency() {
        val view = TunerPreset(3, "Station title").presentation(null)
        assertEquals("Station title", view.station)
        assertNull(view.frequency)
    }
    @Test fun spotifyMetadataAndXmlEntities() {
        val info = parser.spotify(spotify("<Playback_Info>Play</Playback_Info><Meta_Info><Artist>A &amp; B</Artist><Track>Track</Track><Album>Album</Album></Meta_Info>"))
        assertEquals("A & B", info.artist)
        assertEquals("Track", info.title)
        assertEquals("Album", info.album)
        assertEquals(PlaybackState.PLAYING, info.playback)
    }
    @Test fun spotifyPhysicalNotReadyHasNoFakeMetadata() {
        val info = parser.spotify(fixture("rn301-spotify-not-ready.xml"))
        assertEquals("Not Ready", info.availability)
        assertEquals(PlaybackState.STOPPED, info.playback)
        assertNull(info.title); assertNull(info.artist); assertNull(info.album)
        assertNull(info.shuffle); assertNull(info.repeat)
    }
    @Test fun spotifyPauseAndUnknownRemainDistinct() {
        assertEquals(PlaybackState.PAUSED, parser.spotify(spotify("<Playback_Info>Pause</Playback_Info>")).playback)
        val unknown = parser.spotify(spotify("<Playback_Info>Buffering</Playback_Info>"))
        assertEquals(PlaybackState.UNKNOWN, unknown.playback)
        assertEquals("Buffering", unknown.playbackState)
        assertEquals(PlaybackState.UNKNOWN, parser.spotify(spotify("")).playback)
    }
    @Test fun optionalModesAreParsedIndependently() {
        val info = parser.spotify(spotify("<Play_Mode><Shuffle>On</Shuffle><Repeat>One</Repeat></Play_Mode>"))
        assertEquals(true, info.shuffle); assertEquals(RepeatMode.ONE, info.repeat)
        val bad = parser.spotify(spotify("<Play_Mode><Shuffle>Maybe</Shuffle><Repeat>New</Repeat></Play_Mode>"))
        assertNull(bad.shuffle); assertEquals(RepeatMode.UNKNOWN, bad.repeat)
        val off = parser.spotify(spotify("<Play_Mode><Shuffle>Off</Shuffle><Repeat>All</Repeat></Play_Mode>"))
        assertEquals(false, off.shuffle); assertEquals(RepeatMode.ALL, off.repeat)
    }
    @Test fun spotifyRejectsMalformedEnvelopeAndWrongSource() {
        for (bad in listOf("<broken", "<YAMAHA_AV rsp=\"GET\" RC=\"0\"><SERVER><Play_Info/></SERVER></YAMAHA_AV>",
            "<YAMAHA_AV rsp=\"PUT\" RC=\"0\"><Spotify><Play_Info/></Spotify></YAMAHA_AV>"))
            assertThrows(YamahaException::class.java) { parser.spotify(bad) }
    }
    @Test fun exactSpotifyRequests() {
        assertEquals("<YAMAHA_AV cmd=\"GET\"><Spotify><Play_Info>GetParam</Play_Info></Spotify></YAMAHA_AV>", xml(YamahaCommand.SpotifyInfo))
        for (action in listOf(PlayerAction.PLAY, PlayerAction.PAUSE, PlayerAction.PREVIOUS, PlayerAction.NEXT)) assertEquals(
            "<YAMAHA_AV cmd=\"PUT\"><Spotify><Play_Control><Playback>${action.wire}</Playback></Play_Control></Spotify></YAMAHA_AV>",
            xml(YamahaCommand.SpotifyControl(action)))
        assertEquals(5, PlayerAction.entries.size)
    }
}
