package com.styl15hh1.rn301controller.data.model

import java.math.BigDecimal

data class ReceiverFrequency(val value: Int, val exponent: Int, val unit: String) {
    val display: String get() = BigDecimal.valueOf(value.toLong(), exponent).stripTrailingZeros().toPlainString() + " " + unit
}
data class TunerPreset(val number: Int, val title: String)
data class TunerStatus(
    val band: String? = null,
    val frequency: ReceiverFrequency? = null,
    val preset: Int? = null,
    val availability: String? = null,
    val tuned: Boolean? = null,
    val stereo: Boolean? = null,
    val tuningState: String? = null,
    val programType: String? = null,
    val fmMode: FmMode? = null,
    /** Raw optional RDS text; no timezone or remaining-time interpretation. */
    val clockTime: String? = null,
    val nowPlaying: NowPlaying = NowPlaying("TUNER")
)
data class TunerState(
    val status: TunerStatus? = null,
    val presets: List<TunerPreset> = emptyList(),
    val presetsLoaded: Boolean = false,
    val error: ReceiverError? = null,
    val presetError: ReceiverError? = null,
    val fmRange: FmRange? = null,
    val amRange: TuningRange? = null,
    val configLoaded: Boolean = false
) {
    fun range(band: TunerBand) = if (band == TunerBand.FM) fmRange else amRange
    val currentRange get() = when(status?.band) { "FM" -> fmRange; "AM" -> amRange; else -> null }
}
enum class SourceCapability { BASIC, TUNER, PLAYER, BROWSER }
enum class SourceIcon { RADIO, DISC, OPTICAL, COAXIAL, LINE, MUSIC, SERVER, INTERNET_RADIO, AIRPLAY, INPUT }

/** Describes source families; does not grant UI controls for unimplemented protocols. */
object SourceProfile {
    fun capabilities(id: String): Set<SourceCapability> = when (id.uppercase()) {
        "TUNER" -> setOf(SourceCapability.TUNER)
        "SPOTIFY" -> setOf(SourceCapability.PLAYER)
        "SERVER", "NET RADIO" -> setOf(SourceCapability.PLAYER, SourceCapability.BROWSER)
        // AirPlay control isn't confirmed for this R-N301; input switching only.
        else -> setOf(SourceCapability.BASIC)
    }
    fun icon(id: String) = when (id.uppercase()) {
        "TUNER" -> SourceIcon.RADIO
        "CD" -> SourceIcon.DISC
        "OPTICAL" -> SourceIcon.OPTICAL
        "COAXIAL" -> SourceIcon.COAXIAL
        "LINE1", "LINE2", "LINE3", "LINE 1", "LINE 2", "LINE 3" -> SourceIcon.LINE
        "SPOTIFY" -> SourceIcon.MUSIC
        "SERVER" -> SourceIcon.SERVER
        "NET RADIO" -> SourceIcon.INTERNET_RADIO
        "AIRPLAY" -> SourceIcon.AIRPLAY
        else -> SourceIcon.INPUT
    }
}
