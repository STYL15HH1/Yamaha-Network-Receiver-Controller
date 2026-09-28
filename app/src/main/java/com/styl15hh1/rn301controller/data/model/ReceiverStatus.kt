package com.styl15hh1.rn301controller.data.model

import java.util.Locale
import kotlin.math.pow

enum class PowerState { ON, STANDBY, UNAVAILABLE }
enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, UNAVAILABLE }

data class Source(val id: String, val title: String = labels[id] ?: id, val selectable: Boolean = true) {
    companion object {
        val labels = linkedMapOf(
            "TUNER" to "Tuner", "CD" to "CD", "OPTICAL" to "Optical", "COAXIAL" to "Coaxial",
            "LINE1" to "Line 1", "LINE2" to "Line 2", "LINE3" to "Line 3",
            "Spotify" to "Spotify", "SERVER" to "Server", "NET RADIO" to "Net Radio", "AirPlay" to "AirPlay"
        )
        val known = labels.map { Source(it.key, it.value) }
    }
}

/** Never label unitless receiver values as decibels. Val and Exp remain native. */
data class Volume(val value: Int, val exponent: Int, val unit: String) {
    val db: Double? get() = if (unit == "dB") value / 10.0.pow(exponent) else null
    val display: String get() = db?.let { String.format(Locale.US, "%.1f dB", it) }
        ?: "$value (native)"
}
data class VolumeRange(val min: Int, val max: Int, val step: Int, val exponent: Int, val unit: String) {
    init { require(min < max && step > 0 && max.toLong() - min <= 10000) }
    fun supports(v: Volume) = v.exponent == exponent && v.unit == unit
    fun accepts(value: Int) = value in min..max && (value - min) % step == 0
}
data class ReceiverStatus(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val address: String = "",
    val modelName: String? = null,
    val firmwareVersion: String? = null,
    val powerState: PowerState = PowerState.UNAVAILABLE,
    val volume: Volume? = null,
    val muted: Boolean? = null,
    val currentSource: Source? = null,
    val sources: List<Source> = Source.known,
    val volumeRange: VolumeRange? = null,
    val error: ReceiverError? = null,
    val notice: String? = null,
    val stale: Boolean = false,
    val inputsConfirmed: Boolean = false
)

enum class ErrorKind {
    TIMEOUT, NETWORK_UNAVAILABLE, RECEIVER_UNAVAILABLE, INVALID_RESPONSE,
    UNSUPPORTED_COMMAND, INVALID_ADDRESS, UNSUPPORTED_DEVICE, COMMAND_FAILED, DISCOVERY_FAILED
}
enum class ErrorContext { CONNECTION, REFRESH, COMMAND, MUTE }
data class ReceiverError(val kind: ErrorKind, val detail: String? = null, val context: ErrorContext? = null) {
    val userMessage: String get() = when (kind) {
        ErrorKind.COMMAND_FAILED -> "Command failed"
        ErrorKind.DISCOVERY_FAILED -> "Discovery failed"
        ErrorKind.TIMEOUT -> "Connection timed out"
        ErrorKind.NETWORK_UNAVAILABLE -> "Network unavailable. Connect to your local network."
        ErrorKind.RECEIVER_UNAVAILABLE -> "Receiver unavailable"
        ErrorKind.INVALID_RESPONSE -> "Invalid Yamaha response"
        ErrorKind.UNSUPPORTED_COMMAND -> "The receiver does not support this command or value."
        ErrorKind.INVALID_ADDRESS -> "Enter a local IPv4 address or hostname without a port or URL."
        ErrorKind.UNSUPPORTED_DEVICE -> "Unsupported Yamaha device. This version supports R-N301."
    }
}
class YamahaException(val error: ReceiverError, cause: Throwable? = null) : Exception(error.detail ?: error.userMessage, cause)
fun invalid(detail: String): Nothing = throw YamahaException(ReceiverError(ErrorKind.INVALID_RESPONSE, detail))
