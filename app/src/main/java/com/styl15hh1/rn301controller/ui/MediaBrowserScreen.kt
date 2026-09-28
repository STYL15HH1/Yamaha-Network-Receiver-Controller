package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable
fun MediaBrowserScreen(vm: ReceiverViewModel, enabled: Boolean, modifier: Modifier = Modifier) {
    val state by vm.browser.collectAsStateWithLifecycle()
    MediaBrowserContent(state, enabled, vm::back, vm::browserHome, vm::refreshBrowser,
        vm::selectMedia, vm::browserPage, vm::home, vm::openPlayer, modifier)
}

@Composable
internal fun MediaBrowserContent(state: BrowserState, enabled: Boolean,
    back: () -> Unit, home: () -> Unit, refresh: () -> Unit, select: (MediaItem) -> Unit,
    page: (Boolean) -> Unit, receiver: () -> Unit, player: () -> Unit, modifier: Modifier = Modifier) {
    val radio = state.source == "NET RADIO"
    val list = state.list
    val canNavigate = enabled && state.phase != BrowserPhase.LOADING
    var searching by remember(list?.source, list?.layer, list?.title, state.navigationPath) { mutableStateOf(false) }
    var query by remember(list?.source, list?.layer, list?.title, state.navigationPath) { mutableStateOf("") }
    val visibleItems = remember(list?.items, query) { RadioMenuPresentation.filter(list?.items.orEmpty(), query) }
    val scroll = rememberLazyListState()
    LaunchedEffect(list?.source, list?.layer, list?.title, query) { scroll.scrollToItem(0) }
    val searchable = radio && list?.aggregated == true && list.items.size > 1
    val searchHint = tr(if (CountryFlags.isCountryMenu(list)) R.string.search_countries
        else if (list?.items?.all { it.playable } == true) R.string.search_stations else R.string.search)
    Column(modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (radio) Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = back, enabled = canNavigate) { Icon(painterResource(R.drawable.ic_back), tr(R.string.back)) }
            Text(Source("NET RADIO").localized(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (searchable) IconButton(onClick = { searching = !searching; query = "" },
                enabled = canNavigate && state.phase == BrowserPhase.CONTENT) {
                Icon(painterResource(R.drawable.ic_search), tr(R.string.search))
            }
            IconButton(onClick = home, enabled = canNavigate) { Icon(painterResource(R.drawable.ic_home), tr(R.string.radio_home)) }
            IconButton(onClick = refresh, enabled = canNavigate) { Icon(painterResource(R.drawable.ic_refresh), tr(R.string.refresh)) }
        }
        list?.title?.takeUnless { radio && it.replace("_", " ").equals("NET RADIO", true) }?.let {
            Text(it, style = if (radio) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall)
        }
        if (radio && searching) OutlinedTextField(value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
            singleLine = true, label = { Text(searchHint) },
            enabled = canNavigate, trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                    Icon(painterResource(R.drawable.ic_close), tr(R.string.clear_search))
                }
            })
        if (!radio) state.navigationPath?.takeIf { it.isNotEmpty() }?.let { Text(tr(R.string.browser_path, it.joinToString(" › ")), style = MaterialTheme.typography.bodySmall) }
        if (!radio) list?.layer?.let { Text(tr(R.string.menu_level, it), style = MaterialTheme.typography.labelMedium) }
        if (!radio) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = back, enabled = canNavigate) {
                Icon(painterResource(R.drawable.ic_back), null); Text(tr(R.string.back))
            }
            OutlinedButton(onClick = home, enabled = canNavigate) {
                Icon(painterResource(R.drawable.ic_home), null); Text(tr(R.string.browser_home))
            }
            TextButton(onClick = refresh, enabled = canNavigate) { Text(tr(R.string.refresh)) }
        }
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
                BrowserFailure.LIMIT_REACHED -> R.string.radio_limit
                BrowserFailure.PLAYBACK_FAILED -> R.string.radio_playback_failed
                else -> if (radio) R.string.radio_failed else R.string.browser_failed
            }))
            BrowserPhase.IDLE -> Text(tr(if (radio) R.string.radio_open_hint else R.string.open_browser_hint))
            BrowserPhase.CONTENT -> Unit
        }
        if (radio && searching && visibleItems.isEmpty() && state.phase == BrowserPhase.CONTENT)
            Text(tr(R.string.no_results))
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("media_menu"), state = scroll,
            verticalArrangement = Arrangement.spacedBy(if (radio) 4.dp else 8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)) {
            items(visibleItems, key = { "${it.originPage ?: 1}:${it.line}" }) { item ->
                val actionable = canNavigate && state.phase == BrowserPhase.CONTENT && item.selectable
                if (radio) CompactRadioRow(list, item, actionable, select)
                else OutlinedCard(Modifier.fillMaxWidth().clickable(enabled = actionable) { select(item) }) {
                    ListItem(
                        headlineContent = { Text(item.title) },
                        leadingContent = {
                            val flag = CountryFlags.forEntry(list, item)
                            if (flag != null) Text(flag, modifier = Modifier.clearAndSetSemantics {},
                                style = MaterialTheme.typography.titleLarge)
                            else Icon(painterResource(when(item.type) {
                                MediaType.DIRECTORY -> R.drawable.ic_folder
                                MediaType.ALBUM -> R.drawable.ic_disc
                                MediaType.TRACK -> R.drawable.ic_music
                                MediaType.STATION -> R.drawable.ic_internet_radio
                                MediaType.UNKNOWN -> R.drawable.ic_input
                            }), null)
                        },
                        trailingContent = {
                            if (item.selectable) Icon(painterResource(if(item.browsable) R.drawable.ic_chevron_right else R.drawable.ic_play_arrow),
                                tr(if(item.browsable) R.string.open_folder else R.string.play))
                        },
                        colors = ListItemDefaults.colors(
                            headlineColor = LocalContentColor.current.copy(alpha = ControlPolicy.contentAlpha(actionable)),
                            leadingIconColor = LocalContentColor.current.copy(alpha = ControlPolicy.contentAlpha(actionable)),
                            trailingIconColor = LocalContentColor.current.copy(alpha = ControlPolicy.contentAlpha(actionable)))
                    )
                }
            }
        }
        if (!radio && list?.page != null && list.pages != null && list.pages!! > 0) {
            Text(tr(R.string.browser_page, list.page!!, list.pages!!), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { page(false) },
                    enabled = canNavigate && state.phase == BrowserPhase.CONTENT && list.previous, modifier = Modifier.weight(1f)) {
                    Text(tr(R.string.previous_page))
                }
                OutlinedButton(onClick = { page(true) },
                    enabled = canNavigate && state.phase == BrowserPhase.CONTENT && list.next, modifier = Modifier.weight(1f)) {
                    Text(tr(R.string.next_page))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = receiver) { Text(tr(R.string.receiver)) }
            TextButton(onClick = player, enabled = enabled) { Text(tr(R.string.now_playing)) }
        }
    }
}
