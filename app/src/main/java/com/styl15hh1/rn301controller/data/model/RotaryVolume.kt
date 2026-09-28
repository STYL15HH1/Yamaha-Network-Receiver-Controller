package com.styl15hh1.rn301controller.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2

/** Application numeric guardrails, not a claim about runtime bounds or the Max sentinel. */
data class NativeVolumeBounds(val min: Int, val max: Int) {
    init { require(min <= max) }
    fun target(current: Int, steps: Int): Int {
        if (current < min && steps <= 0) return current // A reported zero must never jump UP on CCW.
        return (current.toLong() + steps).coerceIn(min.toLong(), max.toLong()).toInt()
    }
}
object RotaryVolumePolicy {
    const val DEGREES_PER_STEP = 12f // 30 steps per revolution; easy to tune after phone testing.
    const val DOCUMENTED_MIN = 1
    const val DOCUMENTED_MAX = 99
    fun bounds(state: ReceiverStatus): NativeVolumeBounds? {
        val v = state.volume ?: return null
        if (v.unit != "" || v.exponent != 0 || v.value !in 0..99) return null
        val advertised = state.volumeRange
        if (advertised != null) {
            if (!advertised.supports(v) || advertised.step != 1) return null
            val min = maxOf(DOCUMENTED_MIN, advertised.min)
            val max = minOf(DOCUMENTED_MAX, advertised.max)
            return if (min <= max && v.value <= max && (v.value >= min || (v.value == 0 && min == 1))) NativeVolumeBounds(min, max) else null
        }
        return NativeVolumeBounds(DOCUMENTED_MIN, DOCUMENTED_MAX)
    }
    fun angle(x: Float, y: Float): Float = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    fun delta(previous: Float, current: Float): Float {
        val raw = current - previous
        val normalized = ((raw + 540f) % 360f + 360f) % 360f - 180f
        return if (normalized == -180f && raw > 0) 180f else normalized
    }
}
data class RotaryVolumeState(val value: Int? = null, val active: Boolean = false, val pending: Boolean = false)

/** Local preview only. Finish emits at most one target; receiver readback resolves pending state. */
class RotaryVolumeController {
    private val mutable = MutableStateFlow(RotaryVolumeState())
    val state = mutable.asStateFlow()
    private var receiver: Int? = null
    private var bounds: NativeVolumeBounds? = null
    private var available = false
    private var lastAngle = 0f
    private var remainder = 0f
    private var startValue = 0
    fun receive(value: Int?, limits: NativeVolumeBounds?, enabled: Boolean) {
        receiver = value; bounds = limits; available = enabled && limits != null && value != null
        if (!available) cancel()
        else if (!state.value.active && !state.value.pending) mutable.value = RotaryVolumeState(value)
    }
    fun begin(angle: Float): Boolean {
        if (!available || state.value.pending || !angle.isFinite()) return false
        startValue = receiver ?: return false
        lastAngle = angle; remainder = 0f
        mutable.value = RotaryVolumeState(startValue, active = true)
        return true
    }
    fun move(angle: Float): Int {
        if (!state.value.active || !angle.isFinite()) return 0
        remainder += RotaryVolumePolicy.delta(lastAngle, angle); lastAngle = angle
        val steps = (remainder / RotaryVolumePolicy.DEGREES_PER_STEP).toInt()
        if (steps == 0) return 0
        remainder -= steps * RotaryVolumePolicy.DEGREES_PER_STEP
        val old = state.value.value ?: return 0
        val target = bounds?.target(old, steps) ?: old
        mutable.value = state.value.copy(value = target)
        return target - old
    }
    fun finish(force: Boolean = false): Int? {
        if (!state.value.active) return null
        val target = state.value.value
        if (!force && target == startValue) { cancel(); return null }
        mutable.value = state.value.copy(active = false, pending = true)
        return target
    }
    fun complete(actual: Int?) { receiver = actual; mutable.value = RotaryVolumeState(actual); remainder = 0f }
    fun cancel() { mutable.value = RotaryVolumeState(receiver); remainder = 0f }
}
