package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.player.*
import org.junit.Assert.*
import org.junit.Test

class V074PlayerTest {
    private fun present(title:String?="Track",artist:String?="Artist",album:String?="Album") =
        PlayerPresentation.from(NowPlaying("Spotify",title=title,artist=artist,album=album))
    @Test fun physicalSpotifyResponseRetainsSourceBrandingWithoutInventingAlbumArtwork() {
        val info=YamahaXmlParser().spotify(javaClass.getResource("/rn301-spotify-physical.xml")!!.readText())
        assertEquals("Ready",info.availability)
        assertEquals("Play",info.playbackState)
        assertEquals(PlaybackState.PLAYING,info.playback)
        assertEquals("Give Me More",info.title);assertEquals("Give Me More",info.album)
        assertEquals("KSK Mix House",info.artist)
        assertEquals("/YamahaRemoteControl/Logos/logo0065.png",info.inputLogo.small)
        assertNull(info.inputLogo.medium);assertNull(info.inputLogo.large)
        assertEquals(listOf("KSK Mix House"),PlayerPresentation.from(info).secondary)
    }
    @Test fun trackMatchingAlbumIsShownOnce() { assertEquals(listOf("Artist"),present(album="Track").secondary) }
    @Test fun artistMatchingAlbumIsShownOnce() { assertEquals(listOf("Artist"),present(album="Artist").secondary) }
    @Test fun emptyAlbumIsOmitted() { assertEquals(listOf("Artist"),present(album="").secondary) }
    @Test fun emptyArtistIsOmitted() { assertEquals(listOf("Album"),present(artist="").secondary) }
    @Test fun whitespaceValuesAreOmittedAndRealValuesTrimmed() {
        val p=present(" Track "," \t","\n")
        assertEquals("Track",p.primary);assertTrue(p.secondary.isEmpty())
    }
    @Test fun caseInsensitiveDeduplicationKeepsFirstSpelling() {
        val p=present(" track ","TRACK","Track")
        assertEquals("track",p.primary);assertTrue(p.secondary.isEmpty());assertNull(p.artist);assertNull(p.album)
    }
    @Test fun distinctMetadataKeepsTrackArtistAlbumRoles() {
        val p=present("Money for Nothing","Dire Straits","Brothers in Arms")
        assertEquals("Money for Nothing",p.primary);assertEquals("Dire Straits",p.artist)
        assertEquals("Brothers in Arms",p.album)
        assertEquals(listOf("Dire Straits","Brothers in Arms"),p.secondary)
    }
    @Test fun onlyTrackIsStable() { assertEquals(emptyList<String>(),present(artist=null,album=null).secondary) }
    @Test fun missingMetadataAndUnknownStateRemainUnknown() {
        val info=NowPlaying("Spotify",playbackState="Unexpected")
        assertEquals(PlaybackState.UNKNOWN,info.playback)
        assertNull(PlayerPresentation.from(info).primary)
        assertFalse(PlayerPresentation.from(info).expanded)
    }
    @Test fun rejectedSpotifyStopDoesNotRemoveServerOrNetRadioStop() {
        assertFalse(PlayerAction.STOP in PlayerSources.controls("Spotify"))
        assertTrue(PlayerAction.STOP in PlayerSources.controls("SERVER"))
        assertTrue(PlayerAction.STOP in PlayerSources.controls("NET RADIO"))
        assertThrows(IllegalArgumentException::class.java) { YamahaXmlBuilder.build(YamahaCommand.SpotifyControl(PlayerAction.STOP)) }
    }
}
