package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.ui.SourceTileLayout
import org.junit.Assert.*
import org.junit.Test

class V076PresentationTest {
    private fun entries(names:List<String>)=names.mapIndexed{i,name->MediaItem(i%8+1,name,"Container","NET RADIO",i/8+1)}
    @Test fun expansionPreferencesSurviveRestartWithoutChangingExistingPreferences() {
        val store=MemorySettingsStore();val first=AppSettings(store)
        assertFalse(first.state.value.volumeExpanded);assertFalse(first.state.value.sourcesExpanded)
        first.quickSources(listOf("CD","Spotify","TUNER","NET RADIO"));first.language("pl")
        first.volumeExpanded(true);first.sourcesExpanded(true)
        val restarted=AppSettings(store)
        assertTrue(restarted.state.value.volumeExpanded);assertTrue(restarted.state.value.sourcesExpanded)
        assertEquals(first.state.value.quickSources,restarted.state.value.quickSources)
        assertEquals("pl",restarted.state.value.language)
        restarted.volumeExpanded(false);restarted.sourcesExpanded(false)
        assertFalse(AppSettings(store).state.value.volumeExpanded)
        assertFalse(AppSettings(store).state.value.sourcesExpanded)
    }
    @Test fun nativeFilterIsCaseInsensitiveAndPreservesWireIdentity() {
        val rows=entries(listOf("Poland","Portugal","Japan"))
        val filtered=RadioMenuPresentation.filter(rows," pOL ")
        assertEquals(listOf(rows.first()),filtered);assertSame(rows.first(),filtered.single())
        assertEquals(rows,RadioMenuPresentation.filter(rows," "))
        assertTrue(RadioMenuPresentation.filter(rows,"unmatched").isEmpty())
    }
    @Test fun fourColumnsAtPhoneWidthAdaptForNarrowScreensAndLargeText() {
        assertEquals(4,SourceTileLayout.columns(379f,1f))
        assertEquals(4,SourceTileLayout.columns(328f,1f))
        assertEquals(2,SourceTileLayout.columns(230f,1f))
        assertEquals(2,SourceTileLayout.columns(379f,2f))
    }
    @Test fun netRadioUsesStationThenArtistAndSongWithoutDuplicatedAlbum() {
        val p=PlayerPresentation.from(NowPlaying("NET RADIO",station=" BBC RADIO 1 DANCE ",
            artist="Disclosure & Fatoumata Diawara",title="Douha (Mali Mali)",album=" douha (mali mali) "))
        assertEquals("BBC RADIO 1 DANCE",p.primary)
        assertEquals(listOf("Disclosure & Fatoumata Diawara","Douha (Mali Mali)"),p.secondary)
    }
    @Test fun stationOnlyAndBlankOrDuplicateMetadataRemainClean() {
        val p=PlayerPresentation.from(NowPlaying("NET RADIO",station="Station",title=" station ",artist=" ",album=""))
        assertEquals("Station",p.primary);assertTrue(p.secondary.isEmpty())
        assertNull(PlayerPresentation.from(NowPlaying("NET RADIO")).primary)
    }
}
