package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.settings.SettingsState
import com.styl15hh1.rn301controller.widget.*
import org.junit.Assert.*
import org.junit.Test

class WidgetMetadataTest {
    private fun model(source: String = "TUNER", tuner: TunerStatus? = null, media: NowPlaying? = null) =
        WidgetPresentation.from(ReceiverStatus(connectionState = ConnectionState.CONNECTED, powerState = PowerState.ON,
            currentSource = Source(source)), TunerState(status = tuner), PlayerState(media), SettingsState())
    private fun WidgetPresentation.details() = tunerDetails?.text({ "Preset $it" }, { it.name })

    @Test fun tunerSourceOnlyHasNoPlaceholders() {
        assertNull(model().metadata); assertNull(model().details())
        assertNull(model(tuner = TunerStatus()).details())
    }
    @Test fun frequencyUsesExistingFrequencyFormatting() {
        assertEquals("97.4 MHz", model(tuner = TunerStatus(frequency = ReceiverFrequency(9740, 2, "MHz"))).details())
        assertEquals("999 kHz", model(tuner = TunerStatus(band = "AM", frequency = ReceiverFrequency(999, 0, "kHz"))).details())
    }
    @Test fun stationAndFrequencyPresetStereoRemainSeparate() {
        val result = model(tuner = TunerStatus(band = "FM", frequency = ReceiverFrequency(9740, 2, "MHz"), preset = 1,
            stereo = true, tuned = true, nowPlaying = NowPlaying("TUNER", station = " TOK FM ")))
        assertEquals("TOK FM", result.metadata)
        assertEquals("97.4 MHz · Preset 1 · STEREO", result.details())
    }
    @Test fun missingFieldsNeverProduceEmptySeparators() {
        for (frequency in listOf(null, ReceiverFrequency(9740, 2, "MHz"))) {
            for (preset in listOf(null, 1)) for (stereo in listOf(null, true)) {
                val result = model(tuner = TunerStatus(frequency = frequency, preset = preset, stereo = stereo)).details()
                if (result != null) {
                    assertFalse(result.startsWith(" · ")); assertFalse(result.endsWith(" · "))
                    assertFalse(result.contains("·  ·"))
                }
            }
        }
        assertNull(model(tuner = TunerStatus(preset = 0)).details())
        assertEquals("Preset 2", model(tuner = TunerStatus(preset = 2)).details())
    }
    @Test fun monoRequiresTunedSignalOrExplicitModeAndAutoIsNotStereo() {
        assertNull(model(tuner = TunerStatus(stereo = false, tuned = false)).details())
        assertEquals("MONO", model(tuner = TunerStatus(stereo = false, tuned = true)).details())
        assertEquals("MONO", model(tuner = TunerStatus(fmMode = FmMode.MONO)).details())
        assertEquals("AUTO", model(tuner = TunerStatus(fmMode = FmMode.AUTO)).details())
        assertNull(model(tuner = TunerStatus(band = "AM", fmMode = FmMode.AUTO)).details())
    }
    @Test fun spotifyTrackAndArtistAreIndependent() {
        for (title in listOf(null, "Money for Nothing")) for (artist in listOf(null, "Dire Straits")) {
            val result = model("Spotify", media = NowPlaying("Spotify", title = title, artist = artist))
            assertEquals(title, result.metadata); assertEquals(artist, result.secondary)
        }
    }
    @Test fun serverArtistAndAlbumOmitBlankValues() {
        val result = model("SERVER", media = NowPlaying("SERVER", title = "Song", artist = "Artist", album = "Album"))
        assertEquals("Song", result.metadata); assertEquals("Artist · Album", result.secondary)
        assertEquals("Album", model("SERVER", media = NowPlaying("SERVER", artist = " ", album = "Album")).secondary)
        assertNull(model("SERVER").secondary)
    }
    @Test fun netRadioStationAndTitleAvoidDuplication() {
        val result = model("NET RADIO", media = NowPlaying("NET RADIO", station = "Station", title = "Song"))
        assertEquals("Station", result.metadata); assertEquals("Song", result.secondary)
        val titleOnly = model("NET RADIO", media = NowPlaying("NET RADIO", station = " ", title = "Song"))
        assertEquals("Song", titleOnly.metadata); assertNull(titleOnly.secondary)
        assertNull(model("NET RADIO").metadata)
    }
    @Test fun simpleInputsDoNotBorrowOtherSourcesMetadata() {
        val result = model("OPTICAL", media = NowPlaying("Spotify", title = "Old song", artist = "Old artist"))
        assertNull(result.metadata); assertNull(result.secondary); assertNull(result.tunerDetails)
    }
}
