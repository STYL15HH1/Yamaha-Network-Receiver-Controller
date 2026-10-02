package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable
fun TunerScreen(vm: ReceiverViewModel, enabled: Boolean) {
    val tuner by vm.tuner.collectAsStateWithLifecycle()
    val receiver by vm.status.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val info = tuner.status
    val active = receiver.currentSource?.id == "TUNER" && receiver.powerState == PowerState.ON
    val ready = enabled && info?.availability == "Ready"
    if (!active) {
        Text(tr(R.string.tuner_inactive))
        Button(onClick = { vm.source("TUNER") },
            enabled = !busy && !receiver.stale && receiver.connectionState == ConnectionState.CONNECTED && receiver.powerState == PowerState.ON) {
            Text(tr(R.string.select_tuner))
        }
    }
    tuner.error?.let { ErrorMessage(it.localized()) }
    TunerPlayer(tuner, ready, vm::setBand, vm::setFmMode, vm::stepPreset)
    Section(tr(R.string.presets)) {
        if (tuner.presetsLoaded) {
            if (tuner.presets.isEmpty()) Text(tr(R.string.no_presets))
            PresetGrid(tuner.presets.map { it.presentation(info) }, ready, vm::preset)
        } else if (tuner.presetError != null) {
            ErrorMessage(tuner.presetError!!.localized())
        } else Text(tr(R.string.waiting_presets))
        TextButton(onClick = vm::refreshTuner, enabled = enabled) { Text(tr(R.string.refresh_tuner)) }
    }
    if (active && info?.band in setOf("FM", "AM")) {
        var manual by remember { mutableStateOf(false) }
        TextButton(onClick = { manual = !manual }) { Text(tr(if (manual) R.string.hide_manual_tuning else R.string.manual_tuning)) }
        if (manual) ManualTunerControls(tuner, ready, vm)
    }
}

@Composable
internal fun PresetGrid(presets: List<PresetPresentation>, enabled: Boolean, select: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = PresetLayout.columns(maxWidth.value, LocalDensity.current.fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { preset -> PresetCard(preset, enabled, Modifier.weight(1f).fillMaxHeight()) { select(preset.number) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun CompactTuner(vm: ReceiverViewModel) {
    val tuner by vm.tuner.collectAsStateWithLifecycle()
    val info = tuner.status
    OutlinedCard(onClick = vm::openTuner, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(sourceDrawable("TUNER")), null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(tr(R.string.tuner), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(painterResource(R.drawable.ic_chevron_right), tr(R.string.tuner))
            }
            TunerHomeInformation(info)
        }
    }
}

@Composable
internal fun TunerInformation(info: TunerStatus?) = TunerMetadata(info, home = false)

@Composable
internal fun TunerPlayer(tuner: TunerState, ready: Boolean, selectBand: (TunerBand) -> Unit,
    selectMode: (FmMode) -> Unit, stepPreset: (Int) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tr(R.string.tuner), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            TunerInformation(tuner.status)
            TunerOptions(tuner, ready, selectBand, selectMode)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val canStep = ready && tuner.presetsLoaded && tuner.presets.isNotEmpty()
                FilledTonalButton(onClick = { stepPreset(-1) }, enabled = canStep,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(painterResource(R.drawable.ic_previous), null)
                    Spacer(Modifier.width(6.dp))
                    Text(tr(R.string.previous))
                }
                FilledTonalButton(onClick = { stepPreset(1) }, enabled = canStep,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(tr(R.string.next))
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.ic_next), null)
                }
            }
        }
    }
}
