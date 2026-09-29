package com.styl15hh1.rn301controller

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="en-rUS-w411dp-h891dp")
class SpotifyHeaderTest {
    @get:Rule val compose=createComposeRule()
    @Test fun wordmarkHasAccessibleNameWithoutDuplicateVisibleLabel() {
        var opened=false
        val actions=mutableListOf<PlayerAction>()
        compose.setContent { ReceiverTheme(true) {
            SourceNowPlayingCard("Spotify", NowPlaying("Spotify",title="Track",artist="Artist",
                album="Track",playbackState="Play",availability="Ready"),null,true,{actions+=it},{opened=true})
        }}
        compose.onNodeWithTag("spotify_now_playing_artwork").assertIsDisplayed()
        compose.onNodeWithContentDescription("Spotify").assertIsDisplayed()
        compose.onNodeWithText("Spotify",useUnmergedTree=true).assertDoesNotExist()
        compose.onAllNodesWithText("Track").assertCountEquals(1)
        compose.onNodeWithText("Artist").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous track").performClick()
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNodeWithContentDescription("Next track").performClick()
        assertEquals(listOf(PlayerAction.PREVIOUS,PlayerAction.PAUSE,PlayerAction.NEXT),actions)
        compose.onNodeWithContentDescription("Open Now Playing").performClick()
        assertTrue(opened)
    }
    @Test fun otherSourceHeadersKeepTheirVisibleLabel() {
        compose.setContent { ReceiverTheme(true) {
            SourceNowPlayingCard("NET RADIO", NowPlaying("NET RADIO",station="Station",
                availability="Ready"),null,true,{},{})
        }}
        compose.onNodeWithText("Net Radio").assertIsDisplayed()
        compose.onNodeWithTag("spotify_now_playing_artwork").assertDoesNotExist()
    }
}
