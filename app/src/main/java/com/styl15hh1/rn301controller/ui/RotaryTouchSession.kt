package com.styl15hh1.rn301controller.ui

import com.styl15hh1.rn301controller.data.model.RotaryVolumePolicy
import kotlin.math.hypot

/** Coordinates are pixels relative to the knob. A captured finger may leave its bounds.
 * Only the tiny angular singularity at the center pauses sampling; it never cancels a drag.
 */
class RotaryTouchSession(
    private val width: Float, private val height: Float,
    private val begin: (Float) -> Boolean, private val move: (Float) -> Int,
    private val finish: () -> Unit, private val cancel: () -> Unit
) {
    var active = false
        private set
    private var previous: Float? = null
    private var unwrapped = 0f
    fun angle(x: Float, y: Float): Float? {
        if (!x.isFinite() || !y.isFinite()) return null
        val dx = x - width / 2; val dy = y - height / 2
        return if (hypot(dx, dy) < minOf(width, height) * .025f) null
        else RotaryVolumePolicy.angle(dx, dy)
    }
    fun down(x: Float, y: Float): Boolean {
        if (active || width <= 0 || height <= 0 || !x.isFinite() || !y.isFinite() ||
            x !in 0f..width || y !in 0f..height) return false
        previous = angle(x, y)
        unwrapped = previous ?: 0f
        active = begin(unwrapped)
        return active
    }
    fun drag(x: Float, y: Float): Int {
        if (!active) return 0
        val current = angle(x, y)
        val old = previous
        previous = current
        // Rebase after passing through center; never invent a 180-degree jump.
        if (old == null || current == null) return 0
        unwrapped += RotaryVolumePolicy.delta(old, current)
        return move(unwrapped)
    }
    fun up(x: Float, y: Float): Int {
        if (!active) return 0
        val steps = drag(x, y)
        active = false
        finish()
        return steps
    }
    fun abort() {
        if (active) { active = false; cancel() }
    }
}
