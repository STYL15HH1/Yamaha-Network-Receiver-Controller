package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import org.junit.Assert.*
import org.junit.Test

class RotaryVolumeTest {
    private fun controller(value:Int=40,min:Int=1,max:Int=99)=RotaryVolumeController().also {
        it.receive(value,NativeVolumeBounds(min,max),true);it.begin(0f)
    }
    @Test fun clockwiseAndCounterclockwiseDeltas() {
        assertEquals(10f,RotaryVolumePolicy.delta(0f,10f),0f)
        assertEquals(-10f,RotaryVolumePolicy.delta(10f,0f),0f)
    }
    @Test fun boundaryCrossingsAreSmallSignedDeltas() {
        assertEquals(2f,RotaryVolumePolicy.delta(359f,1f),0f)
        assertEquals(-2f,RotaryVolumePolicy.delta(1f,359f),0f)
        assertEquals(2f,RotaryVolumePolicy.delta(179f,-179f),0f)
    }
    @Test fun touchAnglesUseScreenClockwiseCoordinates() {
        assertEquals(0f,RotaryVolumePolicy.angle(10f,0f),0f)
        assertEquals(90f,RotaryVolumePolicy.angle(0f,10f),0f)
        assertEquals(-90f,RotaryVolumePolicy.angle(0f,-10f),0f)
    }
    @Test fun lessThanOneStepDoesNotChangeValue() {
        val c=controller();assertEquals(0,c.move(11f));assertEquals(40,c.state.value.value);assertNull(c.finish())
    }
    @Test fun exactStepAndReverseStep() {
        val c=controller();assertEquals(1,c.move(12f));assertEquals(41,c.state.value.value)
        assertEquals(-1,c.move(0f));assertEquals(40,c.state.value.value)
    }
    @Test fun fractionalMovementAccumulates() {
        val c=controller()
        assertEquals(0,c.move(4f));assertEquals(0,c.move(8f));assertEquals(1,c.move(12f))
    }
    @Test fun multipleAndRapidStepsDoNotQueueWrites() {
        val c=controller()
        assertEquals(7,c.move(90f));assertEquals(8,c.move(180f))
        assertEquals(55,c.state.value.value);assertEquals(55,c.finish())
        assertTrue(c.state.value.pending);assertNull(c.finish())
    }
    @Test fun rotationAcrossZeroMaintainsAccumulation() {
        val c=controller();c.cancel();c.begin(359f);c.move(1f);c.move(11f)
        assertEquals(41,c.state.value.value)
    }
    @Test fun safeMinimumClampsWithoutStoredOvershoot() {
        val c=controller(2);assertEquals(-1,c.move(-36f));assertEquals(1,c.state.value.value)
        assertEquals(1,c.move(-24f));assertEquals(2,c.state.value.value)
    }
    @Test fun safeMaximumAndConfiguredMaximumClamp() {
        val c=controller(98);assertEquals(1,c.move(36f));assertEquals(99,c.finish())
        val limited=controller(49,max=50);limited.move(36f);assertEquals(50,limited.finish())
    }
    @Test fun reportedZeroNeverIncreasesFromCounterclockwiseMovement() {
        val c=controller(0);assertEquals(0,c.move(-24f));assertNull(c.finish())
        c.begin(0f);assertEquals(1,c.move(12f));assertEquals(1,c.finish())
    }
    @Test fun idleExternalReceiverValueWins() {
        val c=controller();c.cancel();c.receive(48,NativeVolumeBounds(1,99),true)
        assertEquals(48,c.state.value.value)
    }
    @Test fun activePreviewIgnoresPollUntilCompleted() {
        val c=controller();c.move(60f);c.receive(39,NativeVolumeBounds(1,99),true)
        assertEquals(45,c.state.value.value);assertEquals(45,c.finish())
        c.receive(39,NativeVolumeBounds(1,99),true);assertEquals(45,c.state.value.value)
        c.complete(44);assertEquals(44,c.state.value.value);assertFalse(c.state.value.pending)
    }
    @Test fun failedWriteReconcilesToActualValue() {
        val c=controller();c.move(24f);c.finish();c.complete(40)
        assertEquals(40,c.state.value.value);assertFalse(c.state.value.pending)
    }
    @Test fun cancelRestoresLatestReceiverValue() {
        val c=controller();c.move(24f);c.receive(43,NativeVolumeBounds(1,99),true);c.cancel()
        assertEquals(43,c.state.value.value);assertFalse(c.state.value.active);assertNull(c.finish())
    }
    @Test fun unavailableOrInvalidAnglesCannotChangeVolume() {
        val c=controller();assertEquals(0,c.move(Float.NaN));c.receive(null,null,false)
        assertFalse(c.begin(0f));assertNull(c.state.value.value)
    }
    @Test fun rangeIsDocumentedGuardrailUnlessMatchingRuntimeRangeExists() {
        val s=ReceiverStatus(volume=Volume(40,0,""))
        assertEquals(NativeVolumeBounds(1,99),RotaryVolumePolicy.bounds(s))
        assertEquals(NativeVolumeBounds(1,55),RotaryVolumePolicy.bounds(s.copy(volumeRange=VolumeRange(0,55,1,0,""))))
        assertNull(RotaryVolumePolicy.bounds(s.copy(volume=Volume(-300,1,"dB"))))
        assertNull(RotaryVolumePolicy.bounds(s.copy(volumeRange=VolumeRange(50,99,1,0,""))))
        assertNull(RotaryVolumePolicy.bounds(s.copy(volumeRange=VolumeRange(0,99,2,0,""))))
    }
}
