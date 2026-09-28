package com.styl15hh1.rn301controller.data.model

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

data class LiveVolumeResult(val target: Int? = null, val error: ReceiverError? = null)

/** One worker, one latest target, no FIFO of volume values. Accessed from the ViewModel main thread. */
class LiveVolumeWriter(
    private val scope: CoroutineScope,
    private val write: suspend (() -> Int?) -> LiveVolumeResult,
    private val reconcile: suspend (ReceiverError?) -> Unit,
    private val settled: () -> Unit
) {
    companion object { const val INTERVAL_MS = 125L }
    @Volatile private var latest = 0
    private var released = false
    private var worker: Job? = null
    private var wake = Channel<Unit>(Channel.CONFLATED)
    val running: Boolean get() = worker?.isCompleted == false

    fun begin(value: Int): Boolean {
        if (worker?.isCompleted == false) return false
        latest = value; released = false
        wake = Channel(Channel.CONFLATED)
        worker = scope.launch {
            var acknowledged = value
            var failure: ReceiverError? = null
            try {
                while (isActive) {
                    wake.receive()
                    if (!released) delay(INTERVAL_MS)
                    val finalAttempt = released
                    // Read latest inside the repository lock, not before waiting for it.
                    val result = write { latest.takeIf { it != acknowledged } }
                    if (result.error == null && result.target != null) {
                        acknowledged = result.target
                        failure = null
                    } else if (result.error != null) failure = result.error
                    if (released) {
                        if (latest != acknowledged && !finalAttempt) {
                            wake.trySend(Unit) // One final synchronization after an in-flight live attempt.
                        } else {
                            reconcile(failure)
                            break
                        }
                    }
                }
            } finally {
                wake.close()
                settled()
            }
        }
        return true
    }
    fun offer(value: Int) {
        if (!running || released || value == latest) return
        latest = value
        wake.trySend(Unit)
    }
    fun finish(value: Int) {
        if (!running || released) return
        latest = value; released = true; wake.trySend(Unit)
    }
    fun cancel() { worker?.cancel(); wake.close() }
}
