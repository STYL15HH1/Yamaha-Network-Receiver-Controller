package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.RotaryVolumePolicy
import com.styl15hh1.rn301controller.data.model.RotaryVolumeState
import kotlin.math.*

@Composable
fun RotaryVolumeControl(
    state: RotaryVolumeState, enabled: Boolean, begin: (Float) -> Boolean, move: (Float) -> Int,
    finish: () -> Unit, cancel: () -> Unit, precision: (Int) -> Unit, showValue: Boolean = true
) {
    val latestBegin by rememberUpdatedState(begin)
    val latestMove by rememberUpdatedState(move)
    val latestFinish by rememberUpdatedState(finish)
    val latestCancel by rememberUpdatedState(cancel)
    val haptics = LocalHapticFeedback.current
    val label = tr(R.string.rotary_volume)
    val hint = tr(R.string.rotate_hint)
    val down = tr(R.string.volume_down)
    val up = tr(R.string.volume_up)
    val value = state.value?.toString() ?: tr(R.string.unavailable)
    val colors = MaterialTheme.colorScheme
    Box(Modifier.widthIn(max = 220.dp).fillMaxWidth().aspectRatio(1f).alpha(ControlPolicy.contentAlpha(enabled || state.pending))
        .semantics(mergeDescendants = true) {
            contentDescription = "$label. $hint"
            stateDescription = value
            if (!enabled) disabled()
            customActions = if (enabled) listOf(
                CustomAccessibilityAction(down) { precision(-1); true },
                CustomAccessibilityAction(up) { precision(1); true }
            ) else emptyList()
        }
        .onKeyEvent {
            if (enabled && it.type == KeyEventType.KeyDown) when (it.key) {
                Key.DirectionUp, Key.DirectionRight -> { precision(1); true }
                Key.DirectionDown, Key.DirectionLeft -> { precision(-1); true }
                else -> false
            } else false
        }.focusable(enabled)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val first = awaitFirstDown(requireUnconsumed = false)
                val gesture = RotaryTouchSession(size.width.toFloat(), size.height.toFloat(),
                    { latestBegin(it) }, { latestMove(it) }, { latestFinish() }, { latestCancel() })
                if (gesture.down(first.position.x, first.position.y)) {
                    first.consume()
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == first.id }
                            if (pointer == null || event.changes.count { it.pressed } > 1 || pointer.isConsumed) break
                            val ticks = if (pointer.pressed) gesture.drag(pointer.position.x, pointer.position.y)
                                else gesture.up(pointer.position.x, pointer.position.y)
                            // Consume every captured movement, including center crossings, before the
                            // scrolling parent sees it. Only cancellation/multitouch aborts the preview.
                            pointer.consume()
                            repeat(abs(ticks)) { haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) }
                            if (!pointer.pressed) break
                        }
                    } finally { gesture.abort() }
                }
            }
        }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(colors.surfaceContainerHighest, radius * 0.84f)
            drawCircle(if (state.active) colors.primaryContainer else colors.surfaceContainer, radius * 0.70f)
            drawCircle(colors.outlineVariant, radius * 0.84f, style = Stroke(1.dp.toPx()))
            val sweep = (((state.value ?: 0) * RotaryVolumePolicy.DEGREES_PER_STEP) % 360).toFloat()
            drawArc(colors.primary, -90f, sweep, false,
                topLeft = Offset(radius * 0.16f, radius * 0.16f),
                size = Size(radius * 1.68f, radius * 1.68f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            repeat(30) { tick ->
                val a = Math.toRadians(tick * 12.0 - 90)
                val direction = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(colors.outlineVariant, center + direction * radius * 0.91f,
                    center + direction * radius * 0.96f, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            val angle = Math.toRadians((state.value ?: 0) * RotaryVolumePolicy.DEGREES_PER_STEP.toDouble() - 90)
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(colors.primary, center + direction * radius * 0.54f,
                center + direction * radius * 0.76f, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        }
        if (showValue) Text(value, style = MaterialTheme.typography.displaySmall)
    }
}
