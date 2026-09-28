package com.styl15hh1.rn301controller.data.player

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.YamahaTransport
import com.styl15hh1.rn301controller.data.protocol.*

interface YamahaPlayer {
    suspend fun getNowPlaying(ip: String, source: String): NowPlaying
    suspend fun control(ip: String, source: String, action: PlayerAction)
}
object PlayerSources {
    fun supported(source: String?) = source in setOf("Spotify", "SERVER", "NET RADIO")
    fun controls(source: String?): Set<PlayerAction> = when (source) {
        "Spotify" -> setOf(PlayerAction.PREVIOUS, PlayerAction.PLAY, PlayerAction.PAUSE, PlayerAction.NEXT)
        "SERVER" -> PlayerAction.entries.toSet()
        "NET RADIO" -> setOf(PlayerAction.STOP)
        else -> emptySet()
    }
}
class LegacyYamahaPlayer(private val transport: YamahaTransport, private val parser: YamahaXmlParser) : YamahaPlayer {
    private fun unsupported(): Nothing =
        throw YamahaException(ReceiverError(ErrorKind.UNSUPPORTED_COMMAND, "Player source unsupported"))
    override suspend fun getNowPlaying(ip: String, source: String): NowPlaying = when(source) {
        "Spotify" -> parser.spotify(transport.command(ip, YamahaCommand.SpotifyInfo))
        "NET RADIO" -> parser.netRadio(transport.command(ip, YamahaCommand.NetRadioInfo))
        "SERVER" -> parser.server(transport.command(ip, YamahaCommand.ServerInfo))
        else -> unsupported()
    }
    override suspend fun control(ip: String, source: String, action: PlayerAction) {
        if (action !in PlayerSources.controls(source)) unsupported()
        val command = when(source) {
            "NET RADIO" -> YamahaCommand.NetRadioControl(action)
            "Spotify" -> YamahaCommand.SpotifyControl(action)
            "SERVER" -> YamahaCommand.ServerControl(action)
            else -> unsupported()
        }
        parser.response(transport.command(ip, command), "PUT")
    }
}
