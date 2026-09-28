package com.styl15hh1.rn301controller.data.browser

import com.styl15hh1.rn301controller.data.model.*
import kotlinx.coroutines.withTimeoutOrNull

/** Exact receiver menu names. No URL lookup, fuzzy matching, or catalogue assumptions. */
class RadioPathNavigator(
    private val browser: YamahaMediaBrowser,
    private val maxPages: Int = 64,
    private val maxDepth: Int = 32,
    private val timeoutMs: Long = 120_000
) {
    suspend fun navigatePath(ip: String, path: List<String>, checkActive: suspend () -> Unit = {}): MediaList {
        if (browser.source != "NET RADIO" || path.isEmpty() || path.size > maxDepth ||
            path.any { it.isBlank() || it.length > 1024 })
            throw BrowserException(BrowserFailure.PATH_UNAVAILABLE)
        return withTimeoutOrNull(timeoutMs) {
            checkActive()
            var list = browser.goHome(ip)
            for ((index, name) in path.withIndex()) {
                var visited = 0
                // Home/Return may retain a cursor on a later page.
                while (list.previous) {
                    if (++visited > maxPages) throw BrowserException(BrowserFailure.LIMIT_REACHED)
                    checkActive()
                    list = browser.changePage(ip, list, false)
                }
                var match: Pair<MediaList, MediaItem>? = null
                visited = 0
                while (true) {
                    if (++visited > maxPages) throw BrowserException(BrowserFailure.LIMIT_REACHED)
                    for (item in list.items.filter { it.title == name }) {
                        if (match != null) throw BrowserException(BrowserFailure.PATH_AMBIGUOUS)
                        match = list to item
                    }
                    if (!list.next) break
                    checkActive()
                    list = browser.changePage(ip, list, true)
                }
                val (matchedList, item) = match ?: throw BrowserException(BrowserFailure.PATH_UNAVAILABLE)
                if (if (index == path.lastIndex) !item.playable else !item.browsable)
                    throw BrowserException(BrowserFailure.PATH_UNAVAILABLE)
                while (list.page != matchedList.page) {
                    checkActive()
                    list = browser.changePage(ip, list, false)
                }
                // selectItem revalidates the originally matched page, not a later cached copy.
                checkActive()
                list = browser.selectItem(ip, matchedList, item)
            }
            list
        } ?: throw BrowserException(BrowserFailure.TIMEOUT)
    }
}
