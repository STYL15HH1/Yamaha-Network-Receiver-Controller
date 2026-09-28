package com.styl15hh1.rn301controller.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable
internal fun PowerAction(state: ReceiverStatus, busy: Boolean, power: (Boolean) -> Unit) {
    val on = state.powerState == PowerState.ON
    val powerLabel = tr(R.string.power)
    val description = tr(when (state.powerState) {
        PowerState.ON -> R.string.on
        PowerState.STANDBY -> R.string.standby
        PowerState.UNAVAILABLE -> R.string.unavailable
    })
    FilledTonalIconButton(colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant),
        onClick = { power(!on) },
        enabled = !busy && !state.stale && state.connectionState == ConnectionState.CONNECTED &&
            state.powerState != PowerState.UNAVAILABLE,
        modifier = Modifier.semantics { stateDescription = "$powerLabel: $description" }) {
        Icon(painterResource(R.drawable.ic_power), tr(if (on) R.string.standby else R.string.power_on),
            tint = if (on && !busy && !state.stale) MaterialTheme.colorScheme.primary else LocalContentColor.current)
    }
}
