package com.styl15hh1.rn301controller.data.model

/** Shared, trimmed metadata with case-insensitive deduplication outside Compose. */
data class PlayerPresentation(val primary: String?, val secondary: List<String>, val expanded: Boolean,
    val artist: String? = null, val album: String? = null) {
    companion object {
        fun from(info: NowPlaying?): PlayerPresentation {
            fun String?.visible() = this?.trim()?.takeIf { it.isNotEmpty() }
            val primary = info?.station.visible() ?: info?.title.visible()
            val seen = mutableSetOf<String>()
            fun unique(value: String?): String? = value.visible()?.takeIf {
                seen.add(it.lowercase(java.util.Locale.ROOT))
            }
            unique(primary)
            val title = unique(info?.title)
            val artist = unique(info?.artist)
            val album = unique(info?.album)
            val secondary = if (info?.source == "NET RADIO") listOfNotNull(artist, title, album)
                else listOfNotNull(title, artist, album)
            return PlayerPresentation(primary, secondary,
                primary != null || secondary.isNotEmpty() || info?.playback in setOf(PlaybackState.PLAYING, PlaybackState.PAUSED),
                artist, album)
        }
    }
}
object PresetLayout {
    fun columns(widthDp: Float, fontScale: Float) = when {
        fontScale > 1.8f -> (widthDp / 120f).toInt().coerceIn(1, 3)
        else -> (widthDp / 100f).toInt().coerceIn(1, 6)
    }
}
