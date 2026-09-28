package com.styl15hh1.rn301controller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.ui.RotaryVolumeControl
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.*

/** Exercises the actual Compose pointer modifier inside the Home scroll-container shape. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp")
class RotaryTouchTest {
    @get:Rule val compose = createComposeRule()
    private val controller = RotaryVolumeController()
    private val writes = mutableListOf<Int>()
    private var enabled by mutableStateOf(true)
    private fun setup() {
        controller.receive(40, NativeVolumeBounds(1, 99), true)
        compose.setContent {
            val state by controller.state.collectAsState()
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    RotaryVolumeControl(state, enabled, controller::begin, controller::move,
                        { controller.finish()?.let { writes += it; controller.complete(it) } },
                        controller::cancel, {})
                    Spacer(Modifier.height(1500.dp))
                }
            }
        }
    }
    private fun knob() = compose.onNode(hasContentDescription("Rotary volume", substring = true))
    private fun TouchInjectionScope.point(degrees: Int, radius: Float): Offset {
        val a = Math.toRadians(degrees.toDouble())
        return center + Offset(cos(a).toFloat(), sin(a).toFloat()) * (width * radius)
    }
    @Test fun innerKnobClockwiseDragCommitsOnce() {
        setup()
        knob().performTouchInput {
            down(point(0, .12f))
            for (a in 6..60 step 6) moveTo(point(a, .12f), 20)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(45), writes) }
    }
    @Test fun releaseAtCenterDoesNotDiscardPreview() {
        setup()
        knob().performTouchInput {
            down(point(0, .35f))
            for (a in 6..60 step 6) moveTo(point(a, .35f), 20)
            moveTo(center)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(45), writes) }
    }
    @Test fun counterClockwiseDragDecreasesVolume() {
        setup()
        knob().performTouchInput {
            down(point(0, .32f))
            for (a in -6 downTo -60 step 6) moveTo(point(a, .32f), 20)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(35), writes) }
    }
    @Test fun centerStartCanMoveOutThenRotate() {
        setup()
        knob().performTouchInput {
            down(center)
            moveTo(point(0, .32f))
            for (a in 6..60 step 6) moveTo(point(a, .32f), 20)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(45), writes) }
    }
    @Test fun capturedGestureMayLeaveTheKnob() {
        setup()
        knob().performTouchInput {
            down(point(0, .32f))
            moveTo(point(0, .7f))
            for (a in 6..60 step 6) moveTo(point(a, .7f), 20)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(45), writes) }
    }
    @Test fun cancelRestoresReceiverWithoutWrite() {
        setup()
        knob().performTouchInput {
            down(point(0, .32f))
            for (a in 6..60 step 6) moveTo(point(a, .32f), 20)
            cancel()
        }
        compose.runOnIdle {
            assertTrue(writes.isEmpty())
            assertEquals(40, controller.state.value.value)
            assertFalse(controller.state.value.active)
        }
    }
    @Test fun additionalFingerCancelsWithoutCommand() {
        setup()
        knob().performTouchInput {
            down(0, point(0, .32f))
            moveTo(0, point(60, .32f), 20)
            down(1, center)
            up(1); up(0)
        }
        compose.runOnIdle { assertTrue(writes.isEmpty()); assertFalse(controller.state.value.active) }
    }
    @Test fun disablingDuringDragCancelsPointerLifecycle() {
        setup()
        knob().performTouchInput {
            down(point(0, .32f)); moveTo(point(60, .32f), 20)
        }
        compose.runOnIdle { assertTrue(controller.state.value.active); enabled = false }
        compose.waitForIdle()
        knob().performTouchInput { up() }
        compose.runOnIdle { assertTrue(writes.isEmpty()); assertFalse(controller.state.value.active) }
    }
    @Test fun unchangedTapSendsNothing() {
        setup()
        knob().performTouchInput { down(center); up() }
        compose.runOnIdle { assertTrue(writes.isEmpty()) }
    }
    @Test fun wraparoundClockwiseKeepsSmallPositiveDelta() {
        setup()
        knob().performTouchInput {
            down(point(350, .32f))
            for (a in 356..416 step 6) moveTo(point(a % 360, .32f), 20)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(45), writes) }
    }
    @Test fun wraparoundCounterClockwiseKeepsSmallNegativeDelta() {
        setup()
        knob().performTouchInput {
            down(point(10, .32f))
            for (a in 4 downTo -56 step 6) moveTo(point((a + 360) % 360, .32f), 20)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(35), writes) }
    }
    @Test fun outerKnobCircularDragWorksInsideScrollableParent() {
        setup()
        knob().performTouchInput {
            down(point(0, .35f))
            for (a in 6..60 step 6) moveTo(point(a, .35f), 20)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(45), writes) }
    }
}
