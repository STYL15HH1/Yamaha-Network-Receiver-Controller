package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.YamahaXmlParser
import org.junit.Assert.*
import org.junit.Test

class PresetFrequencyTest {
    @Test fun fmTitlesPreserveStoredPrecision() {
        assertEquals("97.40 MHz", TunerPreset(1, "1 : FM 97.40 MHz").presentation(null).frequency)
        assertEquals("101.20 MHz", TunerPreset(3, "3 : FM 101.20 MHz").presentation(null).frequency)
    }

    @Test fun amTitleUsesKilohertz() {
        assertEquals("1134 kHz", TunerPreset(8, "8 : AM 1134 kHz").presentation(null).frequency)
    }

    @Test fun unexpectedOrMissingTitlesKeepPresetWithoutLiveFrequencyFallback() {
        for (title in listOf("", "<Title/>", "<Title>Unexpected</Title>", "<Title>3 : FM ? MHz</Title>", "<Title>3 : AM 1134 MHz</Title>")) {
            val xml = """<YAMAHA_AV rsp="GET" RC="0"><Tuner><Play_Control><Preset><Preset_Sel_Item><Item_1><Param>3</Param><RW>RW</RW>$title</Item_1></Preset_Sel_Item></Preset></Play_Control></Tuner></YAMAHA_AV>"""
            val preset = YamahaXmlParser().tunerPresets(xml).single()
            val view = preset.presentation(TunerStatus(preset = 3, frequency = ReceiverFrequency(9740, 2, "MHz")))
            assertEquals(3, view.number)
            assertTrue(view.selected)
            assertNull(view.frequency)
        }
    }
}
