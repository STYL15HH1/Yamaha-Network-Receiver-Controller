package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.player.*
import com.styl15hh1.rn301controller.data.settings.*
import org.junit.Assert.*
import org.junit.Test

class DailyUsePresentationTest {
    @Test fun emptyAndSingleQuickSource() {
        val settings=AppSettings(MemorySettingsStore())
        assertTrue(settings.state.value.quickSources.isEmpty())
        settings.quickSources(listOf("Spotify"))
        assertEquals(listOf("Spotify"),settings.state.value.quickSources)
    }
    @Test fun multipleSourcesPersistAndKeepOrderAcrossRestart() {
        val store=MemorySettingsStore();val settings=AppSettings(store)
        val ids=listOf("NET RADIO","TUNER","LINE1","Spotify")
        settings.quickSources(ids);assertEquals(ids,AppSettings(store).state.value.quickSources)
        settings.quickSources(ids.reversed());assertEquals(ids.reversed(),AppSettings(store).state.value.quickSources)
    }
    @Test fun limitDeduplicationAndUnknownSavedSourcesAreSafe() {
        val settings=AppSettings(MemorySettingsStore())
        settings.quickSources(listOf("CD","CD","Unknown","Spotify","LINE1","TUNER","OPTICAL"))
        assertEquals(4,settings.state.value.quickSources.size)
        val result=QuickSources.available(settings.state.value.quickSources,Source.known)
        assertFalse(result.any{it.id=="Unknown"});assertEquals("CD",result.first().id)
        assertTrue(QuickSources.available(listOf("COAXIAL"),listOf(Source("COAXIAL",selectable=false))).isEmpty())
    }
    @Test fun encodedPreferencePreservesSeparatorsAndUnicodeAndRejectsCorruption() {
        val ids=listOf("Name, with comma","日本 & Radio","%source")
        assertEquals(ids,QuickSources.decode(QuickSources.encode(ids)))
        assertEquals(emptyList<String>(),QuickSources.decode("%bad%"))
    }
    @Test fun lastSourcePersistsWithoutBecomingAnAutomaticSelection() {
        val store=MemorySettingsStore();val settings=AppSettings(store);settings.lastSource("TUNER")
        assertEquals("TUNER",AppSettings(store).state.value.lastSource)
        assertTrue(settings.state.value.quickSources.isEmpty())
    }
    @Test fun activeAndPausedPlayerAreExpandedWithoutMetadata() {
        assertTrue(PlayerPresentation.from(NowPlaying("Spotify",playbackState="Play")).expanded)
        assertTrue(PlayerPresentation.from(NowPlaying("SERVER",playbackState="Pause")).expanded)
    }
    @Test fun stoppedAndMissingMetadataCollapse() {
        assertFalse(PlayerPresentation.from(null).expanded)
        assertFalse(PlayerPresentation.from(NowPlaying("SERVER",playbackState="Stop")).expanded)
        assertFalse(PlayerPresentation.from(NowPlaying("Spotify",title=" ",artist="")).expanded)
    }
    @Test fun stationAndTrackHierarchyIsGenericAndDeduplicated() {
        val info=NowPlaying("NET RADIO",station="Radio",title="Programme",artist="Host",album="Programme")
        val ui=PlayerPresentation.from(info)
        assertEquals("Radio",ui.primary);assertEquals(listOf("Host","Programme"),ui.secondary)
        assertTrue(ui.expanded)
        assertEquals("Track",PlayerPresentation.from(NowPlaying("Spotify",title="Track",artist="Artist")).primary)
    }
    @Test fun supportedCapabilitiesRemainUnchanged() {
        assertEquals(setOf(PlayerAction.PLAY, PlayerAction.PAUSE, PlayerAction.PREVIOUS, PlayerAction.NEXT),PlayerSources.controls("Spotify"))
        assertEquals(PlayerAction.entries.toSet(),PlayerSources.controls("SERVER"))
        assertEquals(setOf(PlayerAction.STOP),PlayerSources.controls("NET RADIO"))
        assertTrue(PlayerSources.controls("TUNER").isEmpty())
    }
    @Test fun rdsFieldsUseVerifiedCapturedGrammarAndOmitBlankData() {
        val xml=javaClass.getResource("/rn301-tuner-not-ready.xml")!!.readText()
            .replace("Not Ready","Ready")
            .replace("<Program_Service></Program_Service>","<Program_Service>Radio Name</Program_Service>")
            .replace("<Radio_Text_A></Radio_Text_A>","<Radio_Text_A>Programme</Radio_Text_A>")
            .replace("<Radio_Text_B></Radio_Text_B>","<Radio_Text_B>Guest</Radio_Text_B>")
            .replace("<Program_Type></Program_Type>","<Program_Type>Culture</Program_Type>")
        val info=YamahaXmlParser().tuner(xml)
        assertEquals("Radio Name",info.nowPlaying.station)
        assertTrue(info.nowPlaying.title!!.contains("Programme"));assertTrue(info.nowPlaying.title.contains("Guest"))
        assertEquals("Culture",info.programType)
        val blank=YamahaXmlParser().tuner(javaClass.getResource("/rn301-tuner-not-ready.xml")!!.readText())
        assertNull(blank.nowPlaying.station);assertNull(blank.nowPlaying.title)
    }
    @Test fun liveStationNameBelongsOnlyToCurrentPreset() {
        val info=TunerStatus(preset=1,frequency=ReceiverFrequency(9020,2,"MHz"),nowPlaying=NowPlaying("TUNER",station="Radio"))
        val current=TunerPreset(1,"FM 90.20 MHz").presentation(info)
        val other=TunerPreset(2,"FM 95.80 MHz").presentation(info)
        assertTrue(current.selected);assertEquals("Radio",current.station)
        assertFalse(other.selected);assertNull(other.station);assertEquals("95.80 MHz",other.frequency)
    }
    @Test fun tunerMetadataCanUseGenericPresentation() {
        val tuner=NowPlaying("TUNER",station="Radio",title="Programme")
        assertEquals("Radio",PlayerPresentation.from(tuner).primary)
    }
    @Test fun presetGridAdaptsToSmallPhoneAndLargeFonts() {
        assertEquals(2,PresetLayout.columns(280f,1f))
        assertEquals(3,PresetLayout.columns(340f,1f))
        assertEquals(6,PresetLayout.columns(650f,1f))
        assertEquals(3,PresetLayout.columns(400f,2f))
    }
}
