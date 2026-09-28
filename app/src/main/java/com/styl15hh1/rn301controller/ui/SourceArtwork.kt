package com.styl15hh1.rn301controller.ui

/** Spotify contains its wordmark; AirPlay resource is symbol-only. */
object SourceArtwork {
    const val spotifyScale = 0.82f
    fun showLabel(source: String) = source != "Spotify"
}
