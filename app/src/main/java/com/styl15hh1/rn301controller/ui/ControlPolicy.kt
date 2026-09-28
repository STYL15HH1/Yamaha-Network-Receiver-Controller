package com.styl15hh1.rn301controller.ui

import com.styl15hh1.rn301controller.data.model.*
import kotlin.math.roundToInt

object ControlPolicy {
    fun powered(state: ReceiverStatus, busy: Boolean) = !busy && !state.stale &&
        state.connectionState == ConnectionState.CONNECTED && state.powerState == PowerState.ON
    fun contentAlpha(enabled: Boolean) = if (enabled) 1f else 0.38f
    fun sliderRange(state: ReceiverStatus): VolumeRange? = state.volumeRange?.takeIf {
        val v = state.volume
        v != null && it.supports(v) && it.accepts(v.value) &&
            (v.unit != "" || (v.exponent == 0 && it.step == 1))
    }
    fun sliderValue(raw: Float, range: VolumeRange): Int =
        (range.min + ((raw - range.min) / range.step).roundToInt() * range.step).coerceIn(range.min, range.max)
}
