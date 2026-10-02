package com.styl15hh1.rn301controller.widget

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.settings.*

data class WidgetPresentation(
    val receiver: String? = null,
    val power: PowerState = PowerState.UNAVAILABLE,
    val source: Source? = null,
    val metadata: String? = null,
    val offline: Boolean = true,
    val favorites: List<Source> = emptyList(),
    val secondary: String? = null,
    val tunerDetails: WidgetTunerDetails? = null
) {
    companion object {
        fun from(status: ReceiverStatus, tuner: TunerState, player: PlayerState, settings: SettingsState): WidgetPresentation {
            val source = status.currentSource
            val metadata = if (status.powerState != PowerState.ON) null else when (source?.id) {
                "TUNER" -> tuner.status?.nowPlaying?.station.takeIf { tuner.error == null }
                "NET RADIO" -> player.nowPlaying?.takeIf { it.source == source.id && player.error == null }?.let { it.station.clean() ?: it.title.clean() }
                "SERVER", "Spotify" -> player.nowPlaying?.takeIf { it.source == source.id && player.error == null }?.title
                else -> null
            }
            val media = player.nowPlaying?.takeIf { it.source == source?.id && player.error == null && status.powerState == PowerState.ON }
            val secondary = when (source?.id) {
                "Spotify" -> media?.artist.clean()
                "SERVER" -> listOfNotNull(media?.artist.clean(), media?.album.clean()).distinct().joinToString(" · ").clean()
                "NET RADIO" -> media?.title.clean()?.takeUnless { it == metadata.clean() }
                else -> null
            }
            val details = tuner.status?.takeIf { source?.id == "TUNER" && status.powerState == PowerState.ON && tuner.error == null }?.let {
                WidgetTunerDetails(it.frequency?.display, it.preset?.takeIf { number -> number > 0 },
                    when {
                        it.band == "AM" -> null
                        it.stereo == true -> WidgetReception.STEREO
                        it.stereo == false && it.tuned == true -> WidgetReception.MONO
                        it.fmMode == FmMode.MONO -> WidgetReception.MONO
                        it.fmMode == FmMode.AUTO -> WidgetReception.AUTO
                        else -> null
                    })
            }
            return WidgetPresentation(status.modelName, status.powerState, source, metadata?.trim()?.takeIf { it.isNotEmpty() },
                status.connectionState != ConnectionState.CONNECTED || status.stale || status.error != null,
                QuickSources.available(QuickSources.ids(settings, status.sources), status.sources).take(4), secondary, details)
        }
    }
}
enum class WidgetAction { REFRESH, POWER, SOURCE }

private fun String?.clean() = this?.trim()?.takeIf { it.isNotEmpty() }

enum class WidgetReception { STEREO, MONO, AUTO }

/** Already-parsed receiver values, kept typed until localized widget rendering. */
data class WidgetTunerDetails(val frequency: String?, val preset: Int?, val reception: WidgetReception?) {
    fun text(presetLabel: (Int) -> String, receptionLabel: (WidgetReception) -> String): String? =
        listOfNotNull(frequency.clean(), preset?.let(presetLabel), reception?.let(receptionLabel))
            .joinToString(" · ").clean()
}
