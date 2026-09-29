package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable
internal fun VolumeControls(state: ReceiverStatus, enabled: Boolean, vm: ReceiverViewModel) {
    val rotary by vm.rotaryState.collectAsStateWithLifecycle()
    val volume = state.volume
    val range = ControlPolicy.sliderRange(state)
    val lower = volume != null && (if (range != null) volume.value > range.min else NativeVolumeStep.target(volume, -1) != null)
    val higher = volume != null && (if (range != null) volume.value < range.max else NativeVolumeStep.target(volume, 1) != null)
    val down = tr(R.string.volume_down)
    val up = tr(R.string.volume_up)
    val summary: @Composable () -> Unit = {
        LargeVolumeValue(if (volume != null && volume.unit.isEmpty() && volume.exponent == 0)
            (if (RotaryVolumePolicy.bounds(state) != null) rotary.value ?: volume.value else volume.value).toString()
            else volume?.localized() ?: tr(R.string.unavailable))
    }
    val controls: @Composable () -> Unit = {
        Box(Modifier.widthIn(max = 170.dp)) {
            if (RotaryVolumePolicy.bounds(state) != null) {
                RotaryVolumeControl(rotary, enabled, vm::beginRotation, vm::rotateVolume,
                    vm::finishRotation, vm::cancelRotation, vm::adjustVolume, showValue = false)
            }
        }
    }
    val actions: @Composable () -> Unit = {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                OutlinedIconButton(onClick = { vm.adjustVolume(-1) }, enabled = enabled && lower && !rotary.active,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = down }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                OutlinedIconButton(onClick = { vm.adjustVolume(1) }, enabled = enabled && higher && !rotary.active,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = up }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            FilledTonalButton(onClick = vm::toggleMute, enabled = enabled && state.muted != null && !rotary.active) {
                Text(tr(when(state.muted) { true -> R.string.unmute; false -> R.string.mute; null -> R.string.mute_unavailable }))
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 280.dp && LocalDensity.current.fontScale <= 1.3f) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(0.4f)) { summary() }
                Box(Modifier.weight(0.6f)) { controls() }
            }
        } else Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            summary()
            controls()
        }
    }
    actions()
    }
}


// Fit the displayed text, not the receiver range. Android font scaling remains respected.
@Composable
internal fun LargeVolumeValue(value: String) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.displayLarge.copy(
        fontSize = 68.sp, lineHeight = 76.sp, fontWeight = FontWeight.SemiBold)
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val available = with(density) { maxWidth.toPx() }
        val fontSize = remember(value, available, density.fontScale, style, measurer) {
            fun fits(size: Float) = measurer.measure(value,
                style = style.copy(fontSize = size.sp, lineHeight = (size * 76 / 68).sp),
                softWrap = false, maxLines = 1).size.width <= available - 2
            if (fits(68f)) 68f else {
                // Android large-font scaling is nonlinear: measure candidates rather than
                // scaling an sp size by a pixel ratio.
                var low = 8f
                var high = 68f
                repeat(8) {
                    val candidate = (low + high) / 2
                    if (fits(candidate)) low = candidate else high = candidate
                }
                low
            }
        }
        Text(value, Modifier.fillMaxWidth(), maxLines = 1, softWrap = false,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = style.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 76 / 68).sp),
            color = MaterialTheme.colorScheme.onSurface)
    }
}
