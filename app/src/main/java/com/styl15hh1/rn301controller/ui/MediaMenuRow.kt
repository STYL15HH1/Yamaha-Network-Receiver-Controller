package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable
internal fun CompactMediaRow(list: MediaList?, item: MediaItem, enabled: Boolean, select: (MediaItem) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(enabled = enabled) { select(item) },
        shape = MaterialTheme.shapes.small, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 4.dp)
            .alpha(ControlPolicy.contentAlpha(enabled)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val flag = CountryFlags.forEntry(list, item)
            if (flag != null) Text(flag, Modifier.width(24.dp).clearAndSetSemantics {},
                style = MaterialTheme.typography.titleLarge)
            else Icon(painterResource(when (item.type) {
                MediaType.DIRECTORY -> R.drawable.ic_folder
                MediaType.ALBUM -> R.drawable.ic_disc
                MediaType.TRACK -> R.drawable.ic_music
                MediaType.STATION -> R.drawable.ic_internet_radio
                MediaType.UNKNOWN -> R.drawable.ic_input
            }), null, Modifier.size(24.dp))
            Text(item.title, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium)
            if (item.selectable) Icon(painterResource(if (item.browsable) R.drawable.ic_chevron_right else R.drawable.ic_play_arrow),
                tr(if (item.browsable) R.string.open_folder else R.string.play), Modifier.size(24.dp))
        }
    }
}
