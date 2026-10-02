package com.styl15hh1.rn301controller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.styl15hh1.rn301controller.data.repository.YamahaRepository
import com.styl15hh1.rn301controller.data.discovery.*
import com.styl15hh1.rn301controller.data.model.ConnectionState
import com.styl15hh1.rn301controller.data.model.PlayerAction
import com.styl15hh1.rn301controller.data.model.MediaItem
import com.styl15hh1.rn301controller.data.model.RotaryVolumeController
import com.styl15hh1.rn301controller.data.model.RotaryVolumePolicy
import com.styl15hh1.rn301controller.data.model.LiveVolumeWriter
import com.styl15hh1.rn301controller.data.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class ReceiverPage { HOME, SETTINGS, ABOUT, TUNER, PLAYER, BROWSER, CONNECTION, COMPATIBILITY }

class ReceiverViewModel(
    private val repository: YamahaRepository,
    private val discovery: YamahaDiscovery? = null,
    val settings: AppSettings = AppSettings(MemorySettingsStore())
) : ViewModel() {
    private val rotary = RotaryVolumeController()
    val rotaryState = rotary.state
    val status = repository.status
    val browser = repository.browser
    val tuner = repository.tuner
    val player = repository.player
    private val liveVolume = LiveVolumeWriter(viewModelScope, repository::writeLiveVolume,
        repository::reconcileLiveVolume) {
        rotary.complete(status.value.volume?.value)
        mutableBusy.value = false
    }
    private val mutableAddress = MutableStateFlow("")
    val address = mutableAddress.asStateFlow()
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableDiscovery = MutableStateFlow(DiscoveryState())
    val discoveryState = mutableDiscovery.asStateFlow()
    private val mutablePage = MutableStateFlow(ReceiverPage.HOME)
    val page = mutablePage.asStateFlow()
    private var foreground: Job? = null
    private var actionJob: Job? = null
    private var scanJob: Job? = null
    private var addressEdited = false
    private var startupAttempted = false
    private val loaded = CompletableDeferred<String>()

    init {
        viewModelScope.launch { status.collect {
            val active = rotaryState.value.active
            rotary.receive(it.volume?.value, RotaryVolumePolicy.bounds(it), ControlPolicy.powered(it, false))
            if (active && !rotaryState.value.active) liveVolume.cancel()
        } }
        viewModelScope.launch {
            val saved = repository.savedAddress()
            if (!addressEdited) mutableAddress.value = saved
            loaded.complete(saved)
        }
    }
    fun editAddress(value: String) { addressEdited = true; mutableAddress.value = value }
    fun connect() {
        startupAttempted = true
        act {
            repository.connect(address.value)
            if (status.value.connectionState == ConnectionState.CONNECTED) home()
        }
    }
    fun reconnect() {
        if (busy.value || liveVolume.running) return
        startupAttempted = true
        act {
            val saved = repository.savedAddress()
            if (saved.isNotBlank()) {
                mutableAddress.value = saved
                repository.connect(saved)
            }
            if (status.value.connectionState != ConnectionState.CONNECTED) {
                mutablePage.value = ReceiverPage.CONNECTION
                repository.showTuner(false); repository.showPlayer(false)
                // act clears busy after return; start the existing finite scan directly here.
                scanInternal()
            }
        }
    }
    fun connect(receiver: DiscoveredReceiver) { editAddress(receiver.address); connect() }
    fun power(on: Boolean) = act { repository.power(on) }
    fun dismissError() = repository.dismissError()
    fun toggleMute() = act { repository.toggleMute() }
    fun source(id: String) = act {
        repository.selectSource(id)
        if (id == "TUNER" && status.value.currentSource?.id == "TUNER" && status.value.error == null) {
            mutablePage.value = ReceiverPage.TUNER
            repository.showTuner(true)
            repository.showPlayer(false)
            repository.refreshTuner()
        } else if (id in setOf("SERVER", "NET RADIO") && status.value.error == null) {
            mutablePage.value = ReceiverPage.BROWSER
            repository.showTuner(false)
            repository.showPlayer(true)
            repository.openBrowser(id)
        }
        if (status.value.error == null && status.value.currentSource?.id == id) settings.lastSource(id)
    }
    fun beginRotation(angle: Float): Boolean {
        if (busy.value || liveVolume.running || !rotary.begin(angle)) return false
        if (!liveVolume.begin(rotaryState.value.value!!)) { rotary.cancel(); return false }
        return true
    }
    fun rotateVolume(angle: Float): Int {
        val steps = rotary.move(angle)
        if (steps != 0) rotaryState.value.value?.let(liveVolume::offer)
        return steps
    }
    fun cancelRotation() {
        liveVolume.cancel()
        rotary.cancel()
    }
    fun finishRotation() {
        val target = rotary.finish(force = true) ?: return
        mutableBusy.value = true
        liveVolume.finish(target)
    }
    fun volume(value: Int) = act { repository.setVolume(value) }
    fun adjustVolume(direction: Int) = act { repository.adjustVolume(direction) }
    fun preset(number: Int) = act { repository.selectPreset(number) }
    fun stepPreset(direction: Int) = act { repository.stepPreset(direction) }
    fun refreshTuner() = act { repository.refreshTuner() }
    fun tuneFm(text: String) = act { repository.tuneFm(text) }
    fun stepFm(direction: Int) = act { repository.stepFm(direction) }
    fun seekFm(up: Boolean) = act { repository.seekFm(up) }
    fun setBand(band: com.styl15hh1.rn301controller.data.model.TunerBand) = act { repository.setBand(band) }
    fun setFmMode(mode: com.styl15hh1.rn301controller.data.model.FmMode) = act { repository.setFmMode(mode) }
    fun tuneFrequency(text: String) = act {
        when(tuner.value.status?.band) { "FM" -> repository.tuneFm(text); "AM" -> repository.tuneAm(text) }
    }
    fun stepFrequency(direction: Int) = act {
        when(tuner.value.status?.band) { "FM" -> repository.stepFm(direction); "AM" -> repository.stepAm(direction) }
    }
    fun seekFrequency(up: Boolean) = act {
        when(tuner.value.status?.band) { "FM" -> repository.seekFm(up); "AM" -> repository.seekAm(up) }
    }
    fun playerAction(action: PlayerAction) = act { repository.controlPlayer(action) }
    private var playerReturn = ReceiverPage.HOME
    fun openPlayer() {
        playerReturn = page.value
        mutablePage.value = ReceiverPage.PLAYER
        repository.showTuner(false)
        repository.showPlayer(true)
        act { repository.refreshPlayer() }
    }
    fun openTuner() {
        mutablePage.value = ReceiverPage.TUNER
        repository.showTuner(true)
        repository.showPlayer(false)
        act { repository.refreshTuner() }
    }
    fun home() {
        mutablePage.value = ReceiverPage.HOME
        repository.showTuner(true)
        repository.showPlayer(true)
    }
    fun selectMedia(item: MediaItem) = act { repository.selectMedia(item) }
    fun browserPage(next: Boolean) = act { repository.browserPage(next) }
    fun browserHome() = act { repository.browserHome() }
    fun refreshBrowser() = act { repository.refreshBrowser() }
    private val mutableReport = MutableStateFlow<com.styl15hh1.rn301controller.data.model.CompatibilityReport?>(null)
    val report = mutableReport.asStateFlow()
    fun compatibility() {
        mutablePage.value = ReceiverPage.COMPATIBILITY
        repository.showTuner(false); repository.showPlayer(false)
    }
    fun generateReport() = act {
        mutableReport.value = null
        mutableReport.value = repository.compatibilityReport(address.value)
    }
    fun about() { mutablePage.value = ReceiverPage.ABOUT }
    fun back() {
        when(page.value) {
            ReceiverPage.ABOUT, ReceiverPage.COMPATIBILITY -> settings()
            ReceiverPage.BROWSER -> {
                if (status.value.currentSource?.id != browser.value.source ||
                    status.value.powerState != com.styl15hh1.rn301controller.data.model.PowerState.ON ||
                    status.value.connectionState != ConnectionState.CONNECTED) home()
                else act { if (repository.browserBack()) home() }
            }
            ReceiverPage.PLAYER -> {
                if (playerReturn == ReceiverPage.BROWSER) mutablePage.value = ReceiverPage.BROWSER else home()
            }
            else -> home()
        }
    }
    fun settings() {
        mutablePage.value = ReceiverPage.SETTINGS
        repository.showTuner(false)
        repository.showPlayer(false)
    }

    fun scan() {
        if (busy.value || scanJob?.isActive == true || discovery == null) return
        scanInternal()
    }
    private fun scanInternal() {
        if (scanJob?.isActive == true || discovery == null) return
        scanJob = viewModelScope.launch {
            mutableDiscovery.value = DiscoveryState(DiscoveryPhase.SEARCHING)
            try { mutableDiscovery.value = discovery.scan() }
            catch (e: CancellationException) { mutableDiscovery.value = DiscoveryState(); throw e }
            catch (_: Exception) { mutableDiscovery.value = DiscoveryState(DiscoveryPhase.ERROR) }
        }
    }

    fun foreground(active: Boolean) {
        foreground?.cancel()
        foreground = null
        if (!active) {
            cancelRotation()
            actionJob?.cancel()
            scanJob?.cancel()
            mutableBusy.value = false
            return
        }
        repository.showTuner(page.value == ReceiverPage.HOME || page.value == ReceiverPage.TUNER)
        repository.showPlayer(page.value == ReceiverPage.HOME || page.value == ReceiverPage.PLAYER || page.value == ReceiverPage.BROWSER)
        foreground = viewModelScope.launch {
            if (discovery != null && !startupAttempted) {
                val saved = loaded.await()
                if (!startupAttempted) {
                    startupAttempted = true
                    if (saved.isNotBlank() && !addressEdited) {
                        mutableBusy.value = true
                        try { repository.connect(saved) } finally { mutableBusy.value = false }
                    }
                    if (status.value.connectionState != ConnectionState.CONNECTED) {
                        mutablePage.value = ReceiverPage.CONNECTION
                        scan()
                    }
                }
            }
            while (isActive) {
                if (!busy.value && !liveVolume.running && scanJob?.isActive != true) repository.refresh()
                delay(if (!status.value.stale && status.value.connectionState == ConnectionState.CONNECTED) 2000 else 8000)
            }
        }
    }
    private fun act(block: suspend () -> Unit) {
        if (busy.value || liveVolume.running || rotary.state.value.active) return
        scanJob?.cancel()
        mutableBusy.value = true
        actionJob = viewModelScope.launch {
            try { block() } finally { mutableBusy.value = false }
        }
    }
}
