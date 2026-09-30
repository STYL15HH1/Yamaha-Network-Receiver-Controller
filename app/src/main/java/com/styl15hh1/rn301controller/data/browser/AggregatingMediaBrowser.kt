package com.styl15hh1.rn301controller.data.browser

import com.styl15hh1.rn301controller.data.model.*
import kotlinx.coroutines.withTimeoutOrNull

/** A UI menu assembled from verified Yamaha windows, never a separate catalogue API.
 * Calls run within the repository mutex. Raw page snapshots are retained for safe selection.
 */
class AggregatingMediaBrowser(
    private val pages: YamahaMediaBrowser,
    private val maxPages: Int = 64,
    private val timeoutMs: Long = 90_000
) : YamahaMediaBrowser {
    override val source = pages.source
    override val navigationPath get() = pages.navigationPath
    override val selectedStation get() = pages.selectedStation
    private var complete: MediaList? = null
    private var snapshots = emptyMap<Int, MediaList>()
    init { require(source in setOf("SERVER", "NET RADIO") && maxPages in 1..64 && timeoutMs > 0) }
    override fun resetPath() { complete = null; snapshots = emptyMap(); pages.resetPath() }
    private fun fail(reason: BrowserFailure): Nothing = throw BrowserException(reason)
    private fun identity(a: MediaList, b: MediaList) =
        a.source == b.source && a.layer == b.layer && a.title == b.title && a.maxLine == b.maxLine
    private fun validate(list: MediaList) {
        if (!list.ready) fail(BrowserFailure.NOT_READY)
        if (list.source != source || list.layer == null || list.maxLine == null) fail(BrowserFailure.INVALID_RESPONSE)
        if ((list.pages ?: 0) > maxPages) fail(BrowserFailure.LIMIT_REACHED)
    }
    private suspend fun step(ip: String, list: MediaList, next: Boolean): MediaList {
        val target = (list.page ?: fail(BrowserFailure.INVALID_RESPONSE)) + if (next) 1 else -1
        val moved = pages.changePage(ip, list, next)
        validate(moved)
        if (!identity(list, moved) || moved.page != target) fail(BrowserFailure.CHANGED)
        return moved
    }
    private suspend fun collect(ip: String, initial: MediaList): MediaList {
        complete = null; snapshots = emptyMap()
        validate(initial)
        var list = initial
        var moves = 0
        while (list.previous) {
            if (++moves >= maxPages) fail(BrowserFailure.LIMIT_REACHED)
            list = step(ip, list, false)
        }
        val collected = linkedMapOf<Int, MediaList>()
        while (true) {
            val number = list.page ?: if (list.maxLine == 0) 1 else fail(BrowserFailure.INVALID_RESPONSE)
            if (collected.size >= maxPages || collected.put(number, list) != null)
                fail(BrowserFailure.LIMIT_REACHED)
            if (!list.next) break
            list = step(ip, list, true)
        }
        // Equal names are not identities. Keep their original page/line so every entry stays selectable.
        val result = collected.values.first().copy(aggregated = true,
            items = collected.flatMap { (page, window) -> window.items.map { it.copy(originPage = page) } })
        snapshots = collected
        complete = result
        return result
    }
    private suspend fun bounded(block: suspend () -> MediaList): MediaList =
        withTimeoutOrNull(timeoutMs) { block() } ?: fail(BrowserFailure.TIMEOUT)
    override suspend fun getCurrentWindow(ip: String) = pages.getCurrentList(ip)
    override suspend fun getCurrentList(ip: String) = bounded { collect(ip, pages.getCurrentList(ip)) }
    override suspend fun goBack(ip: String) = bounded { collect(ip, pages.goBack(ip)) }
    override suspend fun goHome(ip: String) = bounded { collect(ip, pages.goHome(ip)) }
    override suspend fun changePage(ip: String, expected: MediaList, next: Boolean): MediaList =
        fail(BrowserFailure.CHANGED) // There are no UI pages.
    override suspend fun selectItem(ip: String, expected: MediaList, item: MediaItem): MediaList = bounded {
        if (complete != expected || item !in expected.items || !item.selectable) fail(BrowserFailure.CHANGED)
        val target = item.originPage ?: fail(BrowserFailure.CHANGED)
        val snapshot = snapshots[target] ?: fail(BrowserFailure.CHANGED)
        var actual = pages.getCurrentList(ip)
        validate(actual)
        if (!identity(actual, snapshot)) fail(BrowserFailure.CHANGED)
        var moves = 0
        while (actual.page != target) {
            if (++moves >= maxPages) fail(BrowserFailure.LIMIT_REACHED)
            actual = step(ip, actual, (actual.page ?: fail(BrowserFailure.CHANGED)) < target)
        }
        if (actual.items != snapshot.items) fail(BrowserFailure.CHANGED)
        val result = pages.selectItem(ip, snapshot, item.copy(originPage = null))
        if (item.playable && identity(result, snapshot) && result.page == snapshot.page && result.items == snapshot.items)
            expected // Playback did not change the menu. Avoid scanning it again.
        else collect(ip, result)
    }
}
