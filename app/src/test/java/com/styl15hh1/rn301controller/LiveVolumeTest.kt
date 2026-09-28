package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveVolumeTest {
    private class Harness(val scope: TestScope) {
        val writes=mutableListOf<Pair<Long,Int>>()
        var beforeLock=0L
        var responseDelay=0L
        var failures=0
        var readbacks=0
        var finalError:ReceiverError?=null
        var settled=0
        var actual=40
        val writer=LiveVolumeWriter(scope.backgroundScope, { latest ->
            delay(beforeLock)
            val target=latest()
            if(target==null) LiveVolumeResult() else {
                writes+=scope.currentTime to target
                delay(responseDelay)
                if(failures>0) { failures--;LiveVolumeResult(error=ReceiverError(ErrorKind.TIMEOUT)) }
                else { actual=target.coerceAtMost(55);LiveVolumeResult(target) }
            }
        }, { error -> readbacks++;finalError=error }, { settled++ })
        init { writer.begin(40) }
    }
    private fun TestScope.tick(ms:Long=125) { advanceTimeBy(ms);runCurrent() }
    @Test fun writesWhileFingerRemainsActiveAndThrottlesAtNamedInterval()=runTest {
        val h=Harness(this);h.writer.offer(41);runCurrent()
        tick(124);assertTrue(h.writes.isEmpty());tick(1)
        assertEquals(listOf(125L to 41),h.writes)
        assertTrue(h.writer.running);assertEquals(0,h.readbacks)
        h.writer.offer(42);tick(124);assertEquals(1,h.writes.size);tick(1)
        assertEquals(250L to 42,h.writes.last())
        assertEquals(125L,LiveVolumeWriter.INTERVAL_MS)
    }
    @Test fun rapidTargetsAreConflated()=runTest {
        val h=Harness(this);runCurrent()
        for(v in 41..60) h.writer.offer(v)
        tick()
        assertEquals(listOf(125L to 60),h.writes)
    }
    @Test fun slowRequestNeverCreatesBacklogAndReverseWins()=runTest {
        val h=Harness(this);h.responseDelay=500
        h.writer.offer(41);tick()
        for(v in 42..60)h.writer.offer(v)
        h.writer.offer(45);h.writer.finish(45)
        tick(500) // Old in-flight request completes; newest is the only successor.
        assertEquals(listOf(41,45),h.writes.map {it.second})
        tick(500)
        assertEquals(1,h.readbacks);assertFalse(h.writer.running)
    }
    @Test fun latestIsSampledAfterWaitingForRepositoryLock()=runTest {
        val h=Harness(this);h.beforeLock=500
        h.writer.offer(41);tick()
        h.writer.offer(60);tick(500)
        assertEquals(listOf(60),h.writes.map{it.second})
    }
    @Test fun releaseFlushesLatestAndSuppressesDuplicateAcknowledgedTarget()=runTest {
        val h=Harness(this);h.writer.offer(48);tick()
        h.writer.finish(48);runCurrent()
        assertEquals(listOf(48),h.writes.map{it.second})
        assertEquals(1,h.readbacks);assertEquals(1,h.settled)
    }
    @Test fun releaseBeforeTimerStillSendsLatestOnce()=runTest {
        val h=Harness(this);h.writer.offer(45);runCurrent();tick(25)
        h.writer.finish(48);tick(100)
        assertEquals(listOf(48),h.writes.map{it.second})
        assertEquals(1,h.readbacks)
    }
    @Test fun reversingToStartingValueStillRestoresAfterLiveWrite()=runTest {
        val h=Harness(this);h.writer.offer(45);tick()
        h.writer.offer(40);h.writer.finish(40);runCurrent()
        assertEquals(listOf(45,40),h.writes.map{it.second})
    }
    @Test fun failedLiveWriteDoesNotStopSessionAndFinalSynchronizationCanRecover()=runTest {
        val h=Harness(this);h.failures=1;h.writer.offer(48);tick()
        assertTrue(h.writer.running);assertEquals(0,h.readbacks)
        h.writer.finish(48);runCurrent()
        assertEquals(listOf(48,48),h.writes.map{it.second})
        assertNull(h.finalError);assertEquals(1,h.readbacks)
    }
    @Test fun permanentFailureHasBoundedFinalAttemptAndErrorReadback()=runTest {
        val h=Harness(this);h.failures=10;h.writer.offer(48);tick()
        h.writer.finish(48);runCurrent()
        assertEquals(2,h.writes.size);assertEquals(ErrorKind.TIMEOUT,h.finalError?.kind)
        tick(1000);assertEquals(2,h.writes.size);assertFalse(h.writer.running)
    }
    @Test fun cancellationDropsPendingTargetsWithoutUndoingCompletedWrites()=runTest {
        val h=Harness(this);h.writer.offer(45);tick()
        h.writer.offer(60);h.writer.cancel();runCurrent();tick(1000)
        assertEquals(listOf(45),h.writes.map{it.second});assertEquals(45,h.actual)
        assertEquals(0,h.readbacks)
    }
    @Test fun unchangedGestureReadsBackWithoutPut()=runTest {
        val h=Harness(this);h.writer.finish(40);runCurrent()
        assertTrue(h.writes.isEmpty());assertEquals(1,h.readbacks)
    }
}
