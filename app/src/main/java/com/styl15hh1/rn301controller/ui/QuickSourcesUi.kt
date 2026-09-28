package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.Source
import com.styl15hh1.rn301controller.data.settings.*

@Composable
fun QuickSourceChoices(settings: AppSettings, sources: List<Source>, dismiss: () -> Unit) {
    val preferences by settings.state.collectAsState()
    val selectedIds = QuickSources.ids(preferences, sources)
    val choices = (selectedIds.map { id -> sources.firstOrNull { it.id == id } ?: Source(id, selectable = false) } +
        sources.filter { it.selectable && it.id !in selectedIds }).distinctBy { it.id }
    AlertDialog(onDismissRequest = dismiss, title = { Text(tr(R.string.quick_sources)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(tr(R.string.quick_sources_hint, QuickSources.MAX))
                choices.forEach { source ->
                    val index = selectedIds.indexOf(source.id)
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                        val label = source.localized()
                        Checkbox(checked = index >= 0, modifier = Modifier.semantics { contentDescription = label },
                            enabled = index >= 0 || (source.selectable && selectedIds.size < QuickSources.MAX),
                            onCheckedChange = { selected ->
                                settings.quickSources(if (selected) selectedIds + source.id else selectedIds - source.id)
                            })
                        Column(Modifier.weight(1f)) {
                            Text(source.localized())
                            if (!source.selectable) Text(tr(R.string.unavailable), style = MaterialTheme.typography.labelSmall)
                        }
                        if (index >= 0) {
                            IconButton(onClick = {
                                val ids = selectedIds.toMutableList()
                                java.util.Collections.swap(ids, index, index - 1); settings.quickSources(ids)
                            }, enabled = index > 0) {
                                Icon(painterResource(R.drawable.ic_arrow_up), tr(R.string.move_up))
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = dismiss) { Text(tr(R.string.dismiss)) } })
}

@Composable
fun QuickSourceRow(sources: List<Source>, current: Source?, enabled: Boolean, select: (String) -> Unit) {
    val visible = sources.take(QuickSources.MAX)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(Modifier.widthIn(max = (visible.size * 120).dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { source ->
                CompactSourceTile(source, current?.id == source.id, enabled, select,
                    Modifier.weight(1f))
            }
        }
    }
}