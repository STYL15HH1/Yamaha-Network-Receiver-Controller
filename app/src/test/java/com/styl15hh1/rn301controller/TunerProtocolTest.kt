package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import org.junit.Assert.*
import org.junit.Test

class TunerProtocolTest {
    private val parser = YamahaXmlParser()
    @Test fun actualReadOnlyResponse() {
        val tuner = parser.tuner(fixture("rn301-tuner-not-ready.xml"))
        assertEquals("FM", tuner.band)
        assertEquals("97.4 MHz", tuner.frequency!!.display)
        assertEquals(1, tuner.preset)
        assertEquals("Not Ready", tuner.availability)
        assertEquals(false, tuner.tuned)
    }
    @Test fun missingRdsIsNotInvented() {
        val info = parser.tuner(fixture("rn301-tuner-not-ready.xml"))
        assertNull(info.nowPlaying.station)
        assertNull(info.nowPlaying.title)
        assertNull(info.programType)
    }
    @Test fun frequencyUsesExponentNotFixedDivision() {
        val xml = fixture("rn301-tuner-not-ready.xml").replace("<Val>9740</Val><Exp>2</Exp><Unit>MHz</Unit>",
            "<Val>1134</Val><Exp>0</Exp><Unit>kHz</Unit>")
        assertEquals("1134 kHz", parser.tuner(xml).frequency!!.display)
    }
    @Test fun stationAndProgramMetadata() {
        val xml = fixture("rn301-tuner-not-ready.xml")
            .replace("<Program_Service></Program_Service>", "<Program_Service>RADIO &amp; TWO</Program_Service>")
            .replace("<Radio_Text_A></Radio_Text_A>", "<Radio_Text_A>Artist</Radio_Text_A>")
            .replace("<Radio_Text_B></Radio_Text_B>", "<Radio_Text_B>Song</Radio_Text_B>")
        val tuner = parser.tuner(xml)
        assertEquals("RADIO & TWO", tuner.nowPlaying.station)
        assertEquals("Artist · Song", tuner.nowPlaying.title)
    }
    @Test fun noPresetAndMissingFrequencyAreSafe() {
        val xml = """<YAMAHA_AV rsp="GET" RC="0"><Tuner><Play_Info><Preset>
            <Preset_Sel>No Preset</Preset_Sel></Preset></Play_Info></Tuner></YAMAHA_AV>"""
        assertNull(parser.tuner(xml).preset)
        assertNull(parser.tuner(xml).frequency)
    }
    @Test fun invalidFrequencyRejected() {
        assertThrows(YamahaException::class.java) {
            parser.tuner(fixture("rn301-tuner-not-ready.xml").replace("<Exp>2</Exp>", "<Exp>oops</Exp>"))
        }
    }
    @Test fun malformedTunerRejected() {
        assertThrows(YamahaException::class.java) { parser.tuner("<YAMAHA_AV") }
        assertThrows(YamahaException::class.java) { parser.tuner(statusXml()) }
    }
    @Test fun physicalPresetListHasSixNotEight() {
        val presets = parser.tunerPresets(fixture("rn301-presets.xml"))
        assertEquals((1..6).toList(), presets.map { it.number })
        assertEquals("1 : FM 97.40 MHz", presets.first().title)
    }
    @Test fun readOnlyEmptyAndOutOfRangePresetsIgnored() {
        val xml = fixture("rn301-presets.xml").replace("<Param>1</Param>", "<Param>41</Param>")
        assertEquals(5, parser.tunerPresets(xml).size)
    }
    @Test fun exactTunerCommands() {
        assertTrue(YamahaXmlBuilder.build(YamahaCommand.TunerInfo).contains("<Tuner><Play_Info>GetParam</Play_Info></Tuner>"))
        assertTrue(YamahaXmlBuilder.build(YamahaCommand.TunerPresets).contains("<Tuner><Play_Control><Preset><Preset_Sel_Item>GetParam</Preset_Sel_Item></Preset></Play_Control></Tuner>"))
        assertTrue(YamahaXmlBuilder.build(YamahaCommand.SetPreset(4)).contains("<Tuner><Play_Control><Preset><Preset_Sel>4</Preset_Sel></Preset></Play_Control></Tuner>"))
        assertThrows(IllegalArgumentException::class.java) { YamahaXmlBuilder.build(YamahaCommand.SetPreset(41)) }
    }
    @Test fun sourceMappingsAndUnknownInput() {
        assertEquals(SourceIcon.OPTICAL, SourceProfile.icon("OPTICAL"))
        assertEquals(SourceIcon.COAXIAL, SourceProfile.icon("COAXIAL"))
        assertEquals(SourceIcon.LINE, SourceProfile.icon("LINE3"))
        assertEquals(SourceIcon.MUSIC, SourceProfile.icon("Spotify"))
        assertEquals(SourceIcon.INPUT, SourceProfile.icon("Unknown"))
        assertEquals(setOf(SourceCapability.BASIC), SourceProfile.capabilities("AirPlay"))
        assertEquals(setOf(SourceCapability.PLAYER, SourceCapability.BROWSER), SourceProfile.capabilities("SERVER"))
        assertEquals(setOf(SourceCapability.TUNER), SourceProfile.capabilities("TUNER"))
    }
}
internal fun fixture(name: String): String = checkNotNull(TunerProtocolTest::class.java.classLoader?.getResource(name)).readText()
