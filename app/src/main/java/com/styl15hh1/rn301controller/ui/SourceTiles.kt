package com.styl15hh1.rn301controller.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@DrawableRes
fun sourceDrawable(id: String): Int = when (SourceProfile.icon(id)) {
    SourceIcon.RADIO -> R.drawable.ic_radio
    SourceIcon.DISC -> R.drawable.ic_disc
    SourceIcon.OPTICAL -> R.drawable.ic_optical
    SourceIcon.COAXIAL -> R.drawable.ic_coaxial
    SourceIcon.LINE -> R.drawable.ic_line
    SourceIcon.MUSIC -> R.drawable.ic_music
    SourceIcon.SERVER -> R.drawable.ic_server
    SourceIcon.INTERNET_RADIO -> R.drawable.ic_internet_radio
    SourceIcon.AIRPLAY -> R.drawable.airplay_artwork
    SourceIcon.INPUT -> R.drawable.ic_input
}

@Composable
fun SourceTiles(sources: List<Source>, current: Source?, enabled: Boolean, select: (String) -> Unit) {
    val all = if (current != null && sources.none { it.id == current.id }) sources + current.copy(selectable = false) else sources
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = SourceTileLayout.columns(maxWidth.value, LocalDensity.current.fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            all.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { source ->
                        CompactSourceTile(source, current?.id == source.id, enabled, select, Modifier.weight(1f))
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}