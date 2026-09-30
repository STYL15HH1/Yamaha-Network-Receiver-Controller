package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable
fun MediaBrowserScreen(vm: ReceiverViewModel, enabled: Boolean, modifier: Modifier = Modifier) {
    val state by vm.browser.collectAsStateWithLifecycle()
    MediaBrowserContent(state, enabled, vm::back, vm::browserHome, vm::refreshBrowser,
        vm::selectMedia, vm::home, vm::openPlayer, modifier)
}

@Composable
internal fun MediaBrowserContent(state: BrowserState, enabled: Boolean,
    back: () -> Unit, home: () -> Unit, refresh: () -> Unit, select: (MediaItem) -> Unit,
    receiver: () -> Unit, player: () -> Unit, modifier: Modifier = Modifier) {
    val radio = state.source == "NET RADIO"
    val list = state.list
    val canNavigate = enabled && state.phase != BrowserPhase.LOADING
    var searching by remember(list?.source, list?.layer, list?.title, state.navigationPath) { mutableStateOf(false) }
    var query by remember(list?.source, list?.layer, list?.title, state.navigationPath) { mutableStateOf("") }
    val visibleItems = remember(list?.items, query) { RadioMenuPresentation.filter(list?.items.orEmpty(), query) }
    val scroll = rememberLazyListState()
    LaunchedEffect(list?.source, list?.layer, list?.title, query) { scroll.scrollToItem(0) }
    val searchable = list?.aggregated == true && list.items.size > 1
    val searchHint = tr(if (radio && CountryFlags.isCountryMenu(list)) R.string.search_countries
        else if (radio && list?.items?.all { it.playable } == true) R.string.search_stations else R.string.search)
    Column(modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = back, enabled = canNavigate) { Icon(painterResource(R.drawable.ic_back), tr(R.string.back)) }
            Text(Source(state.source).localized(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (searchable) IconButton(onClick = { searching = !searching; query = "" },
                enabled = canNavigate && state.phase == BrowserPhase.CONTENT) {
                Icon(painterResource(R.drawable.ic_search), tr(R.string.search))
            }
            IconButton(onClick = home, enabled = canNavigate) { Icon(painterResource(R.drawable.ic_catalogue_root), tr(if (radio) R.string.radio_root else R.string.server_root)) }
            IconButton(onClick = refresh, enabled = canNavigate) { Icon(painterResource(R.drawable.ic_refresh), tr(R.string.refresh)) }
        }
        list?.title?.takeUnless { it.replace("_", " ").equals(state.source, true) }?.let {
            Text(it, style = MaterialTheme.typography.titleLarge)
        }
        if (searching) OutlinedTextField(value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
            singleLine = true, label = { Text(searchHint) },
            enabled = canNavigate, trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                    Icon(painterResource(R.drawable.ic_close), tr(R.string.clear_search))
                }
            })
        if (!enabled && state.phase != BrowserPhase.LOADING) Text(tr(if (radio) R.string.radio_inactive else R.string.browser_inactive))
        when(state.phase) {
            BrowserPhase.LOADING -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(tr(if (radio) R.string.loading_radio else R.string.loading_media)) }
            BrowserPhase.EMPTY -> Text(tr(if (radio) R.string.no_stations else if (list?.root == true) R.string.no_media_servers else R.string.empty_folder))
            BrowserPhase.ERROR -> ErrorMessage(tr(when(state.error) {
                BrowserFailure.TIMEOUT -> if (radio) R.string.radio_timeout else R.string.browser_timeout
                BrowserFailure.NETWORK -> R.string.network_unavailable
                BrowserFailure.INVALID_RESPONSE -> R.string.browser_invalid
                BrowserFailure.NOT_READY -> R.string.browser_not_ready
                BrowserFailure.CHANGED -> R.string.browser_changed
                BrowserFailure.INACTIVE -> if (radio) R.string.radio_inactive else R.string.browser_inactive
                BrowserFailure.SERVICE_UNAVAILABLE -> if (radio) R.string.radio_service else R.string.browser_not_ready
                BrowserFailure.PATH_UNAVAILABLE -> R.string.radio_path_missing
                BrowserFailure.PATH_AMBIGUOUS -> R.string.radio_path_ambiguous
                BrowserFailure.LIMIT_REACHED -> R.string.browser_limit
                BrowserFailure.PLAYBACK_FAILED -> R.string.radio_playback_failed
                else -> if (radio) R.string.radio_failed else R.string.browser_failed
            }))
            BrowserPhase.IDLE -> Text(tr(if (radio) R.string.radio_open_hint else R.string.open_browser_hint))
            BrowserPhase.CONTENT -> Unit
        }
        if (searching && visibleItems.isEmpty() && state.phase == BrowserPhase.CONTENT)
            Text(tr(R.string.no_results))
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("media_menu"), state = scroll,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 12.dp)) {
            items(visibleItems, key = { "${it.originPage ?: 1}:${it.line}" }) { item ->
                val actionable = canNavigate && state.phase == BrowserPhase.CONTENT && item.selectable
                CompactMediaRow(list, item, actionable, select)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = receiver) { Text(tr(R.string.receiver)) }
            TextButton(onClick = player, enabled = enabled) { Text(tr(R.string.now_playing)) }
        }
    }
}
