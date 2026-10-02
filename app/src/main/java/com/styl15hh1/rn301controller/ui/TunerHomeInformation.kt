package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.TunerStatus

/** Only display normalization: receiver RDS/state remains untouched. */
internal fun tunerMetadataKey(text: String): String =
    text.trim().replace(',', '.').replace(Regex("\\s+"), "").lowercase()

@Composable
internal fun ColumnScope.TunerHomeInformation(info: TunerStatus?) = TunerMetadata(info, home = true)

@Composable
internal fun TunerMetadata(info: TunerStatus?, home: Boolean) {
    val frequency = info?.frequency?.display
    val seen = mutableSetOf<String>()
    frequency?.let { seen.add(tunerMetadataKey(it)) }
    fun unique(text: String?): String? = text?.trim()?.takeIf {
        it.isNotEmpty() && seen.add(tunerMetadataKey(it))
    }
    // The existing parser maps only RDS Program_Service to station.
    // Never promote Radio Text (title) to station when Program Service is absent.
    val station = unique(info?.nowPlaying?.station)
    val radioText = unique(info?.nowPlaying?.title)
    val preset = info?.preset?.let { tr(R.string.preset_number, it) }
    val details = if (home) listOfNotNull(info?.band, tunerSignal(info), preset).distinct()
        else listOfNotNull(info?.band, preset ?: tr(R.string.no_preset), tunerSignal(info)).distinct()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (home) {
            station?.let { Text(it, style = MaterialTheme.typography.titleLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Text(frequency ?: tr(R.string.frequency_unavailable),
                style = if (station != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                station?.let { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(frequency ?: tr(R.string.frequency_unavailable),
                    style = if (station != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!home && details.isNotEmpty()) Text(details.joinToString(" · "),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        radioText?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (home) 2 else 1, overflow = TextOverflow.Ellipsis) }
        if (home && details.isNotEmpty()) Text(details.joinToString(" · "),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val status = when {
            info == null -> tr(R.string.waiting_tuner)
            info.availability != "Ready" -> tr(R.string.tuner_not_ready)
            info.tuningState != null -> tr(R.string.seeking)
            else -> null
        }
        status?.let { Text(it, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
