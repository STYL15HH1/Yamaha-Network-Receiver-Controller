package com.styl15hh1.rn301controller.data.browser

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.YamahaTransport
import com.styl15hh1.rn301controller.data.protocol.*
import kotlinx.coroutines.delay

interface YamahaMediaBrowser {
    val source: String
    val navigationPath: List<String>? get() = null
    val selectedStation: RadioFavorite? get() = null
    fun resetPath() {}
    suspend fun getCurrentList(ip: String): MediaList
    suspend fun getCurrentWindow(ip: String): MediaList = getCurrentList(ip)
    suspend fun selectItem(ip: String, expected: MediaList, item: MediaItem): MediaList
    suspend fun goBack(ip: String): MediaList
    suspend fun goHome(ip: String): MediaList
    suspend fun changePage(ip: String, expected: MediaList, next: Boolean): MediaList
}
class BrowserException(val failure: BrowserFailure) : Exception(failure.name)

/** Stateful Yamaha menus stay on the receiver. Called under the repository transaction lock. */
class ServerMediaBrowser(transport: YamahaTransport, parser: YamahaXmlParser) :
    LegacyMediaBrowser(transport, parser, false)
class NetRadioMediaBrowser(transport: YamahaTransport, parser: YamahaXmlParser, log: (String) -> Unit = {}) :
    LegacyMediaBrowser(transport, parser, true, log)

open class LegacyMediaBrowser(
    private val transport: YamahaTransport, private val parser: YamahaXmlParser,
    private val radio: Boolean, private val log: (String) -> Unit = {}
) : YamahaMediaBrowser {
    override val source = if (radio) "NET RADIO" else "SERVER"
    final override var navigationPath: List<String>? = null
        private set
    final override var selectedStation: RadioFavorite? = null
        private set
    private var observed: MediaList? = null
    override fun resetPath() { navigationPath = null; selectedStation = null; observed = null }
    private val listCommand get() = if (radio) YamahaCommand.NetRadioList else YamahaCommand.ServerList
    private val backCommand get() = if (radio) YamahaCommand.NetRadioBack else YamahaCommand.ServerBack
    private fun record(list: MediaList): MediaList {
        if (radio) {
            if (list.root) navigationPath = emptyList()
            else if (observed?.let { it.layer != list.layer || it.title != list.title } != false) {
                navigationPath = null; selectedStation = null
            }
            observed = list
        }
        return list
    }
    private suspend fun readReady(ip: String, transition: ((MediaList) -> Boolean)? = null): MediaList {
        val attempts = if (radio) 20 else 6
        repeat(attempts) { attempt ->
            log("$source List_Info request")
            val xml = transport.command(ip, listCommand)
            val list = if (radio) parser.netRadioList(xml) else parser.serverList(xml)
            log("$source menu=${list.menuStatus} layer=${list.layer} cursor=${list.currentLine}/${list.maxLine} page=${list.page}")
            if (list.ready && (transition == null || transition(list))) return list
            if (!list.ready && list.menuStatus !in setOf("Busy", "Loading"))
                throw BrowserException(if (radio) BrowserFailure.SERVICE_UNAVAILABLE else BrowserFailure.NOT_READY)
            if (attempt < attempts - 1) delay(if (radio) 1000 else 500)
        }
        throw BrowserException(BrowserFailure.NOT_READY)
    }
    override suspend fun getCurrentList(ip: String): MediaList {
        return record(readReady(ip))
    }
    private suspend fun put(ip: String, command: YamahaCommand) {
        parser.response(transport.command(ip, command), "PUT")
        delay(700) // Receiver-served controller refreshes List_Info after 700 ms.
    }
    private suspend fun confirm(ip: String, expected: MediaList): MediaList {
        val actual = getCurrentList(ip)
        if (actual.source != expected.source || actual.layer != expected.layer || actual.title != expected.title ||
            actual.page != expected.page || actual.maxLine != expected.maxLine || actual.items != expected.items)
            throw BrowserException(BrowserFailure.CHANGED)
        return actual
    }
    override suspend fun selectItem(ip: String, expected: MediaList, item: MediaItem): MediaList {
        confirm(ip, expected)
        if (!item.selectable || item !in expected.items) throw BrowserException(BrowserFailure.CHANGED)
        val parent = navigationPath
        selectedStation = null
        log("$source select line=${item.line} type=${item.type} name=${item.title.take(120).replace(Regex("[\\r\\n]"), " ")}")
        put(ip, if (radio) YamahaCommand.NetRadioSelect(item.line) else YamahaCommand.ServerSelect(item.line))
        if (item.playable) {
            // R-N301 integration follows Direct_Sel with Play for both sources.
            parser.response(transport.command(ip, if (radio) YamahaCommand.NetRadioControl(PlayerAction.PLAY)
                else YamahaCommand.ServerControl(PlayerAction.PLAY)), "PUT")
            if (radio) waitForPlayback(ip)
        }
        val result = if (radio && item.browsable) readReady(ip) { it.layer == expected.layer!! + 1 }
            else readReady(ip)
        record(result)
        if (radio && item.browsable && parent != null) navigationPath = parent + item.title
        if (radio && item.playable && parent != null) selectedStation = RadioFavorite(item.title, parent + item.title)
        log("$source path=${navigationPath?.joinToString(" > ")?.take(300)?.replace(Regex("[\\r\\n]"), " ")}")
        return result
    }
    override suspend fun goBack(ip: String): MediaList {
        val before = getCurrentList(ip)
        if (before.root) return before
        put(ip, backCommand)
        val parent = navigationPath?.dropLast(1)
        val result = if (radio) readReady(ip) { it.layer != before.layer } else readReady(ip)
        if (result.layer == null || before.layer == null || result.layer >= before.layer)
            throw BrowserException(BrowserFailure.CHANGED)
        record(result)
        if (radio && result.layer == before.layer - 1) navigationPath = if (result.root) emptyList() else parent
        selectedStation = null
        return result
    }
    override suspend fun goHome(ip: String): MediaList {
        var list = getCurrentList(ip)
        repeat(32) {
            if (list.root) return list
            val layer = list.layer ?: throw BrowserException(BrowserFailure.NOT_READY)
            put(ip, backCommand)
            list = record(if (radio) readReady(ip) { it.layer != layer } else readReady(ip))
            if (list.layer == null || list.layer >= layer) throw BrowserException(BrowserFailure.CHANGED)
        }
        if (list.root) return list
        throw BrowserException(BrowserFailure.NOT_READY)
    }
    private suspend fun waitForPlayback(ip: String) {
        repeat(10) { attempt ->
            val info = parser.netRadio(transport.command(ip, YamahaCommand.NetRadioInfo))
            log("NET RADIO Play_Info state=${info.playbackState} availability=${info.availability}")
            if (info.playback == PlaybackState.PLAYING && info.availability != "Not Ready") return
            if (attempt < 9) delay(1000)
        }
        throw BrowserException(BrowserFailure.PLAYBACK_FAILED)
    }
    override suspend fun changePage(ip: String, expected: MediaList, next: Boolean): MediaList {
        val actual = confirm(ip, expected)
        if (if (next) !actual.next else !actual.previous) throw BrowserException(BrowserFailure.CHANGED)
        put(ip, if (radio) YamahaCommand.NetRadioPage(next) else YamahaCommand.ServerPage(next))
        return record(if (radio) readReady(ip) { it.page != actual.page } else readReady(ip)).also {
            if (it.maxLine != actual.maxLine || it.layer != actual.layer || it.title != actual.title ||
                it.page != actual.page!! + if (next) 1 else -1)
                throw BrowserException(BrowserFailure.CHANGED)
        }
    }
}
