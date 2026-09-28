package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable
internal fun TunerOptions(state: TunerState, enabled: Boolean,
    selectBand: (TunerBand) -> Unit, selectMode: (FmMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        TunerBand.entries.forEach { band ->
            FilterChip(selected = state.status?.band == band.name,
                onClick = { selectBand(band) }, enabled = enabled && state.range(band) != null,
                label = { Text(tr(if (band == TunerBand.FM) R.string.band_fm else R.string.band_am)) })
        }
    }
    if (state.status?.band == "FM") {
        Text(tr(R.string.reception), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            FmMode.entries.forEach { mode ->
                FilterChip(selected = state.status.fmMode == mode, enabled = enabled,
                    onClick = { selectMode(mode) },
                    label = { Text(tr(if (mode == FmMode.AUTO) R.string.reception_auto else R.string.reception_mono)) })
            }
        }
    }
}

/** Reception indication always comes from Signal_Info, never the selected FM mode. */
@Composable
internal fun tunerSignal(info: TunerStatus?): String? = when {
    info == null || info.availability != "Ready" -> null
    info.tuned == false -> tr(R.string.no_signal)
    info.tuned == true && info.band == "FM" && info.stereo == true -> tr(R.string.tuned_stereo)
    info.tuned == true && info.band == "FM" && info.stereo == false -> tr(R.string.tuned_mono)
    info.tuned == true -> tr(R.string.tuned)
    else -> null
}
