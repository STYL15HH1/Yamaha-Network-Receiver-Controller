package com.styl15hh1.rn301controller.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.Source

/** Shared compact Favorites and Sources visual treatment. */
@Composable
internal fun CompactSourceTile(source: Source, selected: Boolean, enabled: Boolean,
    select: (String) -> Unit, modifier: Modifier = Modifier) {
    val label = source.localized()
    val height = (68 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerLow)
    FilterChip(shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (selected && enabled) colors.primary else colors.outlineVariant),
        colors = FilterChipDefaults.filterChipColors(containerColor = container,
            selectedContainerColor = container, selectedLabelColor = colors.primary,
            selectedLeadingIconColor = colors.primary,
            disabledContainerColor = colors.surfaceContainerLow,
            disabledSelectedContainerColor = colors.surfaceContainerLow),
        selected = selected, enabled = enabled && source.selectable,
        modifier = modifier.height(height).semantics { contentDescription = label },
        onClick = { select(source.id) },
        label = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                if (source.id != "Spotify")
                    Icon(painterResource(sourceDrawable(source.id)), null, Modifier.size(22.dp))
                if (source.id == "Spotify") Image(painterResource(R.drawable.spotify_wordmark), null,
                    Modifier.widthIn(max = 64.dp).fillMaxWidth().height(24.dp),
                    colorFilter = ColorFilter.tint(LocalContentColor.current), contentScale = ContentScale.Fit)
                else Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall)
            }
        },
        leadingIcon = null)
}

internal object SourceTileLayout {
    fun columns(widthDp: Float, fontScale: Float): Int =
        ((widthDp + 8) / (80 * fontScale.coerceAtLeast(1f))).toInt().coerceIn(1, 4)
}
