package com.styl15hh1.rn301controller.data.model

enum class PlaybackState { PLAYING, PAUSED, STOPPED, UNKNOWN }
enum class RepeatMode { OFF, ONE, ALL, UNKNOWN }
enum class PlayerAction(val wire: String) {
    PLAY("Play"), PAUSE("Pause"), STOP("Stop"), PREVIOUS("Skip Rev"), NEXT("Skip Fwd")
}
data class NowPlaying(
    val source: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val station: String? = null,
    val playbackState: String? = null,
    val shuffle: Boolean? = null,
    val repeat: RepeatMode? = null,
    val availability: String? = null,
    val inputLogo: InputLogo = InputLogo()
) {
    val playback: PlaybackState get() = when (playbackState) {
        "Play" -> PlaybackState.PLAYING
        "Pause" -> PlaybackState.PAUSED
        "Stop" -> PlaybackState.STOPPED
        else -> PlaybackState.UNKNOWN
    }
}
/** Receiver source branding paths, never album artwork; not fetched automatically. */
data class InputLogo(val small: String? = null, val medium: String? = null, val large: String? = null)

data class PlayerState(val nowPlaying: NowPlaying? = null, val error: ReceiverError? = null)

/** Native integer steps: no percent/dB conversion and no inferred receiver Max setting. */
object NativeVolumeStep {
    fun target(current: Volume, direction: Int): Volume? {
        if (direction !in listOf(-1, 1) || current.unit != "" || current.exponent != 0) return null
        // R-N301 manual documents numeric 1..99 and a separate Max sentinel.
        // Zero is the primary integration's minimum. Do not guess Max's wire value.
        val next = current.value + direction
        return current.copy(value = next).takeIf { current.value in 0..99 && next in 0..99 }
    }
}
