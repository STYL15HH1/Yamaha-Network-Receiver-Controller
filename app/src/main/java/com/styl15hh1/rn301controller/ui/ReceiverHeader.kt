package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

/** The only top app bar. Feature navigation lives below the receiver shell. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceiverHeader(state: ReceiverStatus, busy: Boolean, connect: () -> Unit,
    power: (Boolean) -> Unit, settings: () -> Unit) {
    TopAppBar(title = {
        Column {
            Text(state.modelName ?: "R-N301", style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).background(if (state.connectionState == ConnectionState.CONNECTED)
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
            Text(when(state.connectionState) {
                ConnectionState.CONNECTED -> tr(R.string.connected_at, state.address)
                ConnectionState.CONNECTING -> tr(R.string.connecting)
                ConnectionState.UNAVAILABLE -> tr(R.string.receiver_unavailable)
                ConnectionState.DISCONNECTED -> tr(R.string.disconnected)
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), actions = {
        if (state.connectionState != ConnectionState.CONNECTED) IconButton(onClick = connect, enabled = !busy) {
            Icon(painterResource(R.drawable.ic_connect), tr(R.string.connect_receiver))
        }
        PowerAction(state, busy, power)
        IconButton(onClick = settings) { Icon(painterResource(R.drawable.ic_settings), tr(R.string.settings)) }
    })
}
