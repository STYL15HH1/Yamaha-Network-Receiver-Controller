package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.Source

/** Expansion is persisted by AppSettings independently of receiver state. */
@Composable
internal fun ExpandableSources(sources: List<Source>, current: Source?, enabled: Boolean,
    expanded: Boolean, onExpandedChange: (Boolean) -> Unit, select: (String) -> Unit) {
    val action = tr(if (expanded) R.string.collapse_sources else R.string.expand_sources)
    Column {
        TextButton(onClick = { onExpandedChange(!expanded) }, modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = action
        }) {
            Text(tr(R.string.sources), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(painterResource(R.drawable.ic_chevron_right), null,
                Modifier.rotate(if (expanded) -90f else 90f))
        }
        if (expanded) SourceTiles(sources, current, enabled, select)
    }
}
