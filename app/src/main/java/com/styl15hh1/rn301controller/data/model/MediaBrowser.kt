package com.styl15hh1.rn301controller.data.model

enum class MediaType { DIRECTORY, ALBUM, TRACK, STATION, UNKNOWN }
data class MediaItem(val line: Int, val title: String, val attribute: String?, val source: String = "SERVER", val originPage: Int? = null) {
    // The R-N301 distinguishes containers/items, not albums or playlists.
    val type get() = when(attribute) { "Container" -> MediaType.DIRECTORY; "Item" -> if (source == "NET RADIO") MediaType.STATION else MediaType.TRACK; else -> MediaType.UNKNOWN }
    val browsable get() = attribute == "Container"
    val playable get() = attribute == "Item"
    val selectable get() = browsable || playable
}
data class MediaList(
    val source: String, val menuStatus: String, val layer: Int?, val title: String?,
    val currentLine: Int?, val maxLine: Int?, val items: List<MediaItem>, val aggregated: Boolean = false
) {
    val ready get() = menuStatus == "Ready"
    val root get() = layer == 1
    val page get() = currentLine?.takeUnless { aggregated }?.takeIf { it > 0 }?.let { (it - 1) / WINDOW + 1 }
    val pages get() = maxLine?.takeUnless { aggregated }?.let { (it + WINDOW - 1) / WINDOW }
    val previous get() = ready && (page ?: 1) > 1
    val next get() = ready && page != null && pages != null && page!! < pages!!
    companion object { const val WINDOW = 8 }
}
/** A receiver menu identity, never a filesystem path or stream URL. */
data class RadioFavorite(val displayName: String, val navigationPath: List<String>)

enum class BrowserPhase { IDLE, LOADING, EMPTY, CONTENT, ERROR }
enum class BrowserFailure { TIMEOUT, NETWORK, INVALID_RESPONSE, NOT_READY, LOAD_FAILED, CHANGED, INACTIVE, PATH_UNAVAILABLE, PATH_AMBIGUOUS, LIMIT_REACHED, PLAYBACK_FAILED, SERVICE_UNAVAILABLE }
data class BrowserState(
    val source: String = "SERVER", val phase: BrowserPhase = BrowserPhase.IDLE,
    val list: MediaList? = null, val error: BrowserFailure? = null,
    val navigationPath: List<String>? = null, val selectedStation: RadioFavorite? = null
)
