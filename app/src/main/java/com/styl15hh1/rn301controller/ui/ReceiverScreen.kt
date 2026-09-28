package com.styl15hh1.rn301controller.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.player.PlayerSources
import com.styl15hh1.rn301controller.data.settings.AppSettings
import com.styl15hh1.rn301controller.data.settings.QuickSources
import com.styl15hh1.rn301controller.data.discovery.DiscoveryPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverScreen(vm: ReceiverViewModel, settings: AppSettings, selectLanguage: (String) -> Unit) {
    val state by vm.status.collectAsStateWithLifecycle()
    val preferences by settings.state.collectAsStateWithLifecycle()
    val browser by vm.browser.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val page by vm.page.collectAsStateWithLifecycle()
    val scan by vm.discoveryState.collectAsStateWithLifecycle()
    val rotary by vm.rotaryState.collectAsStateWithLifecycle()
    val powered = ControlPolicy.powered(state, busy)
    BackHandler(page != ReceiverPage.HOME) { if (!busy) vm.back() }
    Scaffold(topBar = {
        ReceiverHeader(state, busy || rotary.active, vm::reconnect, vm::power, vm::settings)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (page != ReceiverPage.HOME && !(page == ReceiverPage.BROWSER && browser.source == "NET RADIO")) Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = vm::back, enabled = !busy) {
                    Icon(painterResource(R.drawable.ic_back), tr(R.string.back))
                }
                Text(when(page) {
                    ReceiverPage.SETTINGS -> tr(R.string.settings)
                    ReceiverPage.ABOUT -> tr(R.string.about)
                    ReceiverPage.TUNER -> tr(R.string.tuner)
                    ReceiverPage.PLAYER -> tr(R.string.now_playing)
                    ReceiverPage.BROWSER -> Source(browser.source).localized()
                    ReceiverPage.CONNECTION -> tr(R.string.receiver)
                    ReceiverPage.HOME -> ""
                }, style = MaterialTheme.typography.titleLarge)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it.localized(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::dismissError) { Text(tr(R.string.dismiss)) }
                }
            }
            if (page == ReceiverPage.BROWSER) {
                MediaBrowserScreen(vm, powered && state.currentSource?.id == browser.source, Modifier.weight(1f))
            } else {
                Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when(page) {
                        ReceiverPage.CONNECTION -> ConnectionControls(vm)
                        ReceiverPage.SETTINGS -> SettingsScreen(vm, settings, selectLanguage)
                        ReceiverPage.ABOUT -> { AboutSection(); ReceiverInformation(state) }
                        ReceiverPage.TUNER -> TunerScreen(vm, powered && state.currentSource?.id == "TUNER")
                        ReceiverPage.PLAYER -> PlayerScreen(vm, powered)
                        ReceiverPage.BROWSER -> Unit
                        ReceiverPage.HOME -> {
                            if (state.connectionState != ConnectionState.CONNECTED) {
                                if (scan.phase == DiscoveryPhase.SEARCHING) Text(tr(R.string.searching))
                                if (scan.phase == DiscoveryPhase.FOUND) Text(tr(R.string.receivers_found, scan.receivers.size))
                                TextButton(onClick = vm::reconnect) { Text(tr(R.string.connect_receiver)) }
                            }
                            ExpandableVolume(state, preferences.volumeExpanded, settings::volumeExpanded) {
                                VolumeControls(state, powered, vm)
                            }
                            val quick = QuickSources.available(QuickSources.ids(preferences, state.sources), state.sources)
                            if (quick.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(tr(R.string.favorites), style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    QuickSourceRow(quick, state.currentSource, powered, vm::source)
                                }
                            }
                            val recent = QuickSources.available(listOfNotNull(preferences.lastSource), state.sources)
                                .firstOrNull { it.id != state.currentSource?.id && quick.none { favorite -> favorite.id == it.id } }
                            recent?.let {
                                TextButton(onClick = { vm.source(it.id) }, enabled = powered) {
                                    Text(tr(R.string.last_used_source, it.localized()))
                                }
                            }
                            if (state.powerState == PowerState.ON) {
                                if (PlayerSources.supported(state.currentSource?.id)) CompactNowPlaying(vm, powered)
                                if (state.currentSource?.id == "TUNER") CompactTuner(vm)
                            }
                            ExpandableSources(state.sources, state.currentSource, powered,
                                preferences.sourcesExpanded, settings::sourcesExpanded, vm::source)
                        }
                    }
                    if (page == ReceiverPage.SETTINGS || page == ReceiverPage.ABOUT) Text(tr(R.string.direct_connection), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable
internal fun Section(title: String, centered: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
@Composable internal fun ErrorMessage(message: String) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}
