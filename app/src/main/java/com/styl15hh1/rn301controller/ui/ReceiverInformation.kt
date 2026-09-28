package com.styl15hh1.rn301controller.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

/** Optional cached System/Config information; rendering never issues a network request. */
@Composable
internal fun ReceiverInformation(state: ReceiverStatus) {
    Section(tr(R.string.receiver_information)) {
        state.modelName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        if (state.address.isNotBlank()) Text(state.address)
        Text(tr(if (state.connectionState == ConnectionState.CONNECTED && !state.stale)
            R.string.connected else R.string.disconnected))
        Text(tr(R.string.firmware_version, state.firmwareVersion ?: tr(R.string.unavailable)))
    }
}
