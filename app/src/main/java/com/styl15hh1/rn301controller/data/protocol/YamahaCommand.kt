package com.styl15hh1.rn301controller.data.protocol

import com.styl15hh1.rn301controller.data.model.Volume
import com.styl15hh1.rn301controller.data.model.PlayerAction

enum class NetworkFeature { SERVER, NET_RADIO, Spotify }

sealed interface YamahaCommand {
    /** Read-only Config paths physically observed on the R-N301. */
    data class NetworkConfig(val source: NetworkFeature) : YamahaCommand
    data object Status : YamahaCommand
    data object TunerInfo : YamahaCommand
    data object TunerConfig : YamahaCommand
    data class SetBand(val band: com.styl15hh1.rn301controller.data.model.TunerBand) : YamahaCommand
    data class SetFmMode(val mode: com.styl15hh1.rn301controller.data.model.FmMode) : YamahaCommand
    data class TuneAm(val value: Int) : YamahaCommand
    data class SeekAm(val up: Boolean) : YamahaCommand
    data class TuneFm(val value: Int) : YamahaCommand
    data class SeekFm(val up: Boolean) : YamahaCommand
    data object NetRadioList : YamahaCommand
    data object NetRadioInfo : YamahaCommand
    data class NetRadioSelect(val line: Int) : YamahaCommand
    data object NetRadioBack : YamahaCommand
    data class NetRadioPage(val next: Boolean) : YamahaCommand
    data class NetRadioControl(val action: PlayerAction) : YamahaCommand
    data object ServerList : YamahaCommand
    data class ServerSelect(val line: Int) : YamahaCommand
    data object ServerBack : YamahaCommand
    data class ServerPage(val next: Boolean) : YamahaCommand
    data object ServerInfo : YamahaCommand
    data class ServerControl(val action: PlayerAction) : YamahaCommand
    data object SpotifyInfo : YamahaCommand
    data class SpotifyControl(val action: PlayerAction) : YamahaCommand
    data object TunerPresets : YamahaCommand
    data class SetPreset(val number: Int) : YamahaCommand
    data object Config : YamahaCommand
    data object Inputs : YamahaCommand
    data class Power(val on: Boolean) : YamahaCommand
    data class SetVolume(val volume: Volume) : YamahaCommand
    data class Mute(val on: Boolean) : YamahaCommand
    data class Input(val id: String) : YamahaCommand
}
