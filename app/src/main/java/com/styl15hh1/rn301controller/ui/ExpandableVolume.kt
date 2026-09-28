package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.ReceiverStatus

/** Expansion is a local preference; the header never sends receiver commands. */
@Composable
internal fun ExpandableVolume(state: ReceiverStatus, expanded: Boolean, onExpandedChange: (Boolean) -> Unit,
    controls: @Composable ColumnScope.() -> Unit) {
    val action = tr(if (expanded) R.string.collapse_volume else R.string.expand_volume)
    val mute = tr(when (state.muted) {
        true -> R.string.muted
        false -> R.string.unmuted
        null -> R.string.mute_unavailable
    })
    val value = state.volume?.value?.toString() ?: tr(R.string.unavailable)
    OutlinedCard(Modifier.fillMaxWidth()) {
        TextButton(onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
                contentDescription = action
                stateDescription = "$value · $mute"
            }) {
            Text(tr(R.string.volume), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(Modifier.width(12.dp))
            Text("$value · $mute", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(painterResource(R.drawable.ic_chevron_right), null,
                Modifier.rotate(if (expanded) -90f else 90f))
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp), content = controls)
    }
}
