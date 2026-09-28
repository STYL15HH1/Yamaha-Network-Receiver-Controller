package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.player.PlayerSources

@Composable
fun CompactNowPlaying(vm: ReceiverViewModel, enabled: Boolean) {
    val player by vm.player.collectAsStateWithLifecycle()
    val receiver by vm.status.collectAsStateWithLifecycle()
    val presentation = PlayerPresentation.from(player.nowPlaying)
    if (receiver.currentSource?.id in setOf("Spotify", "NET RADIO")) {
        SourceNowPlayingCard(receiver.currentSource!!.id, player.nowPlaying, player.error?.localized(),
            enabled && player.error == null, vm::playerAction, vm::openPlayer)
        return
    }
    if (!presentation.expanded) {
        OutlinedCard(Modifier.fillMaxWidth().clickable(onClickLabel = tr(R.string.open_player), onClick = vm::openPlayer)) {
            ListItem(
                headlineContent = { Text(Source(receiver.currentSource?.id.orEmpty()).localized()) },
                supportingContent = { Text(player.error?.localized() ?: playbackLabel(player.nowPlaying)) },
                trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), tr(R.string.open_player)) }
            )
        }
        return
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().clickable(onClickLabel = tr(R.string.open_player), onClick = vm::openPlayer).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Source(receiver.currentSource?.id.orEmpty()).localized(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            PlayerMetadata(player.nowPlaying)
            Icon(painterResource(R.drawable.ic_chevron_right), tr(R.string.open_player), Modifier.size(20.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            player.error?.let { Text(it.localized(), color = MaterialTheme.colorScheme.error) }
            PlayerControls(player.nowPlaying, enabled && player.error == null, vm::playerAction)
        }
    }
}

@Composable
internal fun SourceNowPlayingCard(source: String, info: NowPlaying?, error: String?, enabled: Boolean,
    action: (PlayerAction) -> Unit, open: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (source == "Spotify") Image(painterResource(R.drawable.spotify_wordmark), null,
                    Modifier.width(64.dp).height(24.dp).alpha(ControlPolicy.contentAlpha(enabled))
                        .testTag("spotify_now_playing_artwork"),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary), contentScale = ContentScale.Fit)
                else Icon(painterResource(sourceDrawable(source)), null, Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(Source(source).localized(), Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = open) {
                    Icon(painterResource(com.styl15hh1.rn301controller.R.drawable.ic_chevron_right), tr(R.string.open_player))
                }
            }
            PlayerMetadata(info)
            error?.let { ErrorMessage(it) }
            PlayerControls(info ?: NowPlaying(source), enabled && info != null, action)
        }
    }
}

@Composable
fun PlayerScreen(vm: ReceiverViewModel, enabled: Boolean) {
    val player by vm.player.collectAsStateWithLifecycle()
    val receiver by vm.status.collectAsStateWithLifecycle()
    val active = PlayerSources.supported(receiver.currentSource?.id) && receiver.powerState == PowerState.ON
    if (!active) {
        Text(tr(R.string.player_inactive))
        return
    }
    Section(Source(receiver.currentSource?.id.orEmpty()).localized(), centered = true) {
        PlayerMetadata(player.nowPlaying)
        player.error?.let { ErrorMessage(it.localized()) }
        PlayerControls(player.nowPlaying, enabled && player.error == null, vm::playerAction)
        if (PlayerSources.controls(receiver.currentSource?.id).size > 1 && PlayerAction.STOP in PlayerSources.controls(receiver.currentSource?.id)) OutlinedButton(onClick = { vm.playerAction(PlayerAction.STOP) },
            enabled = enabled && player.nowPlaying != null && player.error == null && player.nowPlaying?.availability != "Not Ready") {
            Icon(painterResource(R.drawable.ic_stop), null)
            Spacer(Modifier.width(8.dp))
            Text(tr(R.string.stop))
        }
        // Read-only optional metadata. No unverified shuffle/repeat writes.
        player.nowPlaying?.takeUnless { it.source == "Spotify" }?.shuffle?.let { Text(tr(R.string.shuffle_state, if (it) tr(R.string.on) else tr(R.string.off))) }
        player.nowPlaying?.takeUnless { it.source == "Spotify" }?.repeat?.let {
            Text(tr(R.string.repeat_state, when (it) { RepeatMode.OFF -> tr(R.string.off); RepeatMode.ONE -> tr(R.string.one); RepeatMode.ALL -> tr(R.string.all); RepeatMode.UNKNOWN -> tr(R.string.unknown) }))
        }
    }
}

@Composable
private fun PlayerMetadata(info: NowPlaying?) {
    val presentation = PlayerPresentation.from(info)
    presentation.primary?.let {
        Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.headlineSmall)
    }
    if (info?.source == "Spotify") {
        presentation.artist?.let { Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        presentation.album?.let { Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else presentation.secondary.forEach {
        Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 2,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(playbackLabel(info), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun playbackLabel(info: NowPlaying?) = if (info?.availability == "Not Ready") tr(R.string.playback_not_ready)
    else tr(when(info?.playback) {
        PlaybackState.PLAYING -> R.string.playing
        PlaybackState.PAUSED -> R.string.paused
        PlaybackState.STOPPED -> R.string.stopped
        else -> R.string.playback_unknown
    })

@Composable
private fun PlayerControls(info: NowPlaying?, enabled: Boolean, action: (PlayerAction) -> Unit) {
    val ready = enabled && info != null && info.availability != "Not Ready"
    val controls = PlayerSources.controls(info?.source)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (PlayerAction.PREVIOUS in controls) FilledTonalIconButton(onClick = { action(PlayerAction.PREVIOUS) }, enabled = ready, modifier = Modifier.size(56.dp).align(Alignment.CenterVertically)) {
            Icon(painterResource(R.drawable.ic_previous), tr(R.string.previous_track))
        }
        val playing = info?.playback == PlaybackState.PLAYING
        if (PlayerAction.PLAY in controls && PlayerAction.PAUSE in controls) {
        if (info?.source == "Spotify" && info.playback == PlaybackState.UNKNOWN) {
            FilledIconButton(onClick = {}, enabled = false, modifier = Modifier.size(64.dp).align(Alignment.CenterVertically)) {
                Icon(painterResource(R.drawable.ic_play_arrow), tr(R.string.playback_unknown), Modifier.size(32.dp))
            }
        } else if (info?.playback == PlaybackState.UNKNOWN || info == null) {
            // Unknown is never silently presented as Pause/Play. Explicit actions are unambiguous.
            OutlinedButton(onClick = { action(PlayerAction.PLAY) }, enabled = ready) {
                Icon(painterResource(R.drawable.ic_play_arrow), null); Text(tr(R.string.play))
            }
            OutlinedButton(onClick = { action(PlayerAction.PAUSE) }, enabled = ready) {
                Icon(painterResource(R.drawable.ic_pause), null); Text(tr(R.string.pause))
            }
        } else {
            FilledIconButton(onClick = { action(if (playing) PlayerAction.PAUSE else PlayerAction.PLAY) },
                enabled = ready, modifier = Modifier.size(64.dp).align(Alignment.CenterVertically)) {
                Icon(painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                    if (playing) tr(R.string.pause) else tr(R.string.play), modifier = Modifier.size(32.dp))
            }
        }
        }
        if (controls == setOf(PlayerAction.STOP)) {
            FilledTonalButton(onClick = { action(PlayerAction.STOP) }, enabled = ready) {
                Icon(painterResource(R.drawable.ic_stop), null)
                Spacer(Modifier.width(8.dp))
                Text(tr(R.string.stop))
            }
        }
        if (PlayerAction.NEXT in controls) FilledTonalIconButton(onClick = { action(PlayerAction.NEXT) }, enabled = ready, modifier = Modifier.size(56.dp).align(Alignment.CenterVertically)) {
            Icon(painterResource(R.drawable.ic_next), tr(R.string.next_track))
        }
    }
}
