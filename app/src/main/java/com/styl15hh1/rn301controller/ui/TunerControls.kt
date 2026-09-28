package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable
internal fun PresetCard(preset: PresetPresentation, enabled: Boolean, modifier: Modifier = Modifier, select: () -> Unit) {
    val label = tr(R.string.preset_number, preset.number)
    Card(onClick = select, enabled = enabled,
        modifier = modifier.fillMaxWidth().semantics { selected = preset.selected; contentDescription = label },
        colors = CardDefaults.cardColors(
            containerColor = if (preset.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = if (preset.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(if (preset.selected) 2.dp else 1.dp,
            if (preset.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(8.dp),
            contentAlignment = Alignment.Center) {
            Text(preset.number.toString(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
internal fun ManualTunerControls(tuner: TunerState, enabled: Boolean, vm: ReceiverViewModel) {
    val range = tuner.currentRange
    var input by rememberSaveable(tuner.status?.band) { mutableStateOf("") }
    val parsed = range?.parse(input)
    Section(tr(R.string.manual_tuning)) {
        if (range == null) {
            Text(tr(R.string.tuning_range_unavailable))
        } else {
            Text(tr(R.string.fm_range, range.minDisplay, range.maxDisplay, range.stepDisplay),
                style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.stepFrequency(-1) },
                    enabled = enabled && range.stepFrom(tuner.status?.frequency, -1) != null,
                    modifier = Modifier.heightIn(min = 56.dp)) { Text(tr(R.string.step_down, range.stepDisplay)) }
                FilledTonalButton(onClick = { vm.stepFrequency(1) },
                    enabled = enabled && range.stepFrom(tuner.status?.frequency, 1) != null,
                    modifier = Modifier.heightIn(min = 56.dp)) { Text(tr(R.string.step_up, range.stepDisplay)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.seekFrequency(false) }, enabled = enabled, modifier = Modifier.heightIn(min = 56.dp)) {
                    Icon(painterResource(R.drawable.ic_fast_rewind), null)
                    Spacer(Modifier.width(6.dp)); Text(tr(R.string.seek_down))
                }
                OutlinedButton(onClick = { vm.seekFrequency(true) }, enabled = enabled, modifier = Modifier.heightIn(min = 56.dp)) {
                    Icon(painterResource(R.drawable.ic_fast_forward), null)
                    Spacer(Modifier.width(6.dp)); Text(tr(R.string.seek_up))
                }
            }
            OutlinedTextField(value = input, onValueChange = { if (it.length <= 12) input = it },
                label = { Text(tr(R.string.tuning_frequency)) }, singleLine = true, enabled = enabled,
                isError = input.isNotBlank() && parsed == null,
                supportingText = { if (input.isNotBlank() && parsed == null) Text(tr(R.string.tuning_invalid)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.tuneFrequency(input) }, enabled = enabled && parsed != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(tr(R.string.tune_frequency)) }
        }
    }
}
