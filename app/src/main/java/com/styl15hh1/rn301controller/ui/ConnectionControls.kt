package com.styl15hh1.rn301controller.ui

import com.styl15hh1.rn301controller.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.styl15hh1.rn301controller.data.discovery.*

@Composable
fun ConnectionControls(vm: ReceiverViewModel) {
    val scan by vm.discoveryState.collectAsStateWithLifecycle()
    val address by vm.address.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var manual by rememberSaveable { mutableStateOf(false) }
    Section(tr(R.string.discovery)) {
        Text(when (scan.phase) {
            DiscoveryPhase.IDLE -> tr(R.string.find_receiver)
            DiscoveryPhase.SEARCHING -> tr(R.string.searching)
            DiscoveryPhase.FOUND -> if (scan.receivers.size == 1) tr(R.string.receiver_found) else tr(R.string.receivers_found, scan.receivers.size)
            DiscoveryPhase.EMPTY -> tr(R.string.none_found)
            DiscoveryPhase.NETWORK_UNAVAILABLE -> tr(R.string.network_unavailable)
            DiscoveryPhase.ERROR -> tr(R.string.discovery_failed)
        })
        if (scan.phase == DiscoveryPhase.SEARCHING) LinearProgressIndicator(Modifier.fillMaxWidth())
        scan.receivers.forEach { receiver ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(receiver.model, style = MaterialTheme.typography.titleLarge)
                    if (receiver.name != receiver.model) Text(receiver.name)
                    Text(receiver.address)
                    Button(onClick = { vm.connect(receiver) }, enabled = !busy) { Text(tr(R.string.connect)) }
                }
            }
        }
        if (scan.phase == DiscoveryPhase.EMPTY) {
            Text(tr(R.string.discovery_help),
                style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = vm::scan, enabled = !busy && scan.phase != DiscoveryPhase.SEARCHING) { Text(tr(R.string.scan_again)) }
    }
    Section(tr(R.string.manual_connection)) {
        TextButton(onClick = { manual = !manual }) { Text(if (manual) tr(R.string.hide_manual) else tr(R.string.add_manually)) }
        if (manual) {
            OutlinedTextField(value = address, onValueChange = vm::editAddress,
                label = { Text(tr(R.string.receiver_address)) }, placeholder = { Text("192.168.1.55") },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = { Text(tr(R.string.address_hint)) })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.connect() }, enabled = !busy && address.isNotBlank()) { Text(tr(R.string.connect)) }
                OutlinedButton(onClick = { vm.connect() }, enabled = !busy && address.isNotBlank()) { Text(tr(R.string.test_connection)) }
            }
        }
    }
}
