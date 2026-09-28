package com.styl15hh1.rn301controller.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.*

@Composable fun tr(@StringRes id: Int, vararg args: Any): String = stringResource(id, *args)
@Composable fun ReceiverError.localized(): String = tr(when {
    context == ErrorContext.MUTE -> R.string.mute_failed
    context == ErrorContext.COMMAND && kind != ErrorKind.UNSUPPORTED_COMMAND -> R.string.command_failed
    context == ErrorContext.REFRESH && kind != ErrorKind.NETWORK_UNAVAILABLE -> R.string.refresh_failed
    else -> when (kind) {
        ErrorKind.TIMEOUT -> R.string.timeout
        ErrorKind.NETWORK_UNAVAILABLE -> R.string.network_unavailable
        ErrorKind.RECEIVER_UNAVAILABLE -> R.string.receiver_unavailable
        ErrorKind.INVALID_RESPONSE -> R.string.invalid_response
        ErrorKind.UNSUPPORTED_COMMAND -> R.string.unsupported_command
        ErrorKind.INVALID_ADDRESS -> R.string.invalid_address
        ErrorKind.UNSUPPORTED_DEVICE -> R.string.unsupported_device
        ErrorKind.COMMAND_FAILED -> R.string.command_failed
        ErrorKind.DISCOVERY_FAILED -> R.string.discovery_failed
    }
})
@Composable fun Volume.localized(): String = if (unit == "") tr(R.string.native_volume, value) else display

@Composable fun Source.localized(): String {
    if (title != Source.labels[id] && id in Source.labels) return title
    val resource = when (id) {
        "TUNER" -> R.string.tuner; "CD" -> R.string.cd; "OPTICAL" -> R.string.optical
        "COAXIAL" -> R.string.coaxial; "LINE1" -> R.string.line_one; "LINE2" -> R.string.line_two
        "LINE3" -> R.string.line_three; "Spotify" -> R.string.spotify; "SERVER" -> R.string.server
        "NET RADIO" -> R.string.net_radio; "AirPlay" -> R.string.airplay; else -> null
    }
    return resource?.let { tr(it) } ?: title
}
