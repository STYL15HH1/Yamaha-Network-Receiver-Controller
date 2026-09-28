package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.ui.RotaryTouchSession
import org.junit.Assert.*
import org.junit.Test

class RotaryCoordinateTest {
    private val c=RotaryVolumeController().apply { receive(40,NativeVolumeBounds(1,99),true) }
    private val results=mutableListOf<Int>()
    private val g=RotaryTouchSession(220f,220f,c::begin,c::move,{c.finish()?.let {results+=it}},c::cancel)
    @Test fun coordinateAnglesHaveScreenClockwiseOrientation() {
        assertEquals(0f,g.angle(220f,110f)!!,0.01f)
        assertEquals(90f,g.angle(110f,220f)!!,0.01f)
        assertEquals(-90f,g.angle(110f,0f)!!,0.01f)
        assertNull(g.angle(110f,110f))
    }
    @Test fun entireSquareAcceptsStartWithoutIndicatorTargeting() {
        for ((x,y) in listOf(0f to 0f,110f to 110f,220f to 220f,120f to 110f)) {
            assertTrue(g.down(x,y));assertTrue(c.state.value.active);g.abort()
        }
        assertFalse(g.down(-1f,100f));assertFalse(g.down(Float.NaN,100f))
    }
    @Test fun centerCrossingRebasesWithoutPhantomHalfTurn() {
        g.down(130f,110f);g.drag(110f,130f) // 90 degrees, seven steps + remainder
        assertEquals(47,c.state.value.value)
        g.drag(110f,110f);g.drag(90f,110f)
        assertEquals(47,c.state.value.value)
        g.up(90f,110f);assertEquals(listOf(47),results)
    }
    @Test fun completionAndCancellationAreIdempotent() {
        g.down(130f,110f);g.drag(110f,130f);g.up(110f,130f);g.up(110f,130f);g.abort()
        assertEquals(listOf(47),results)
    }
}
