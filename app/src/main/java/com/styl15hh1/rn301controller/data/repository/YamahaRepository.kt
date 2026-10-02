package com.styl15hh1.rn301controller.data.repository

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.player.LegacyYamahaPlayer
import com.styl15hh1.rn301controller.data.player.PlayerSources
import com.styl15hh1.rn301controller.data.browser.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class YamahaRepository(
    private val transport: YamahaTransport,
    private val store: AddressStore,
    private val parser: YamahaXmlParser = YamahaXmlParser(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val log: (String) -> Unit = {}
) {
    private val lock = Mutex()
    private val mutableStatus = MutableStateFlow(ReceiverStatus())
    val status: StateFlow<ReceiverStatus> = mutableStatus.asStateFlow()
    private val radioPages = NetRadioMediaBrowser(transport, parser, log)
    private val browsers = mapOf(
        "SERVER" to AggregatingMediaBrowser(ServerMediaBrowser(transport, parser)),
        "NET RADIO" to AggregatingMediaBrowser(radioPages)
    )
    private val browserApi: YamahaMediaBrowser get() = browsers.getValue(browser.value.source)
    private val mutableBrowser = MutableStateFlow(BrowserState())
    val browser = mutableBrowser.asStateFlow()
    private val playerApi = LegacyYamahaPlayer(transport, parser)
    private val mutablePlayer = MutableStateFlow(PlayerState())
    val player: StateFlow<PlayerState> = mutablePlayer.asStateFlow()
    @Volatile private var playerVisible = false
    fun showPlayer(visible: Boolean) { playerVisible = visible }
    private var ip: String? = null
    private var verified = false
    private var refreshFailures = 0
    private val mutableTuner = MutableStateFlow(TunerState())
    val tuner: StateFlow<TunerState> = mutableTuner.asStateFlow()
    @Volatile private var tunerVisible = false

    fun dismissError() { mutableStatus.value = status.value.copy(error = null) }
    fun showTuner(visible: Boolean) { tunerVisible = visible }

    suspend fun savedAddress() = store.read()

    suspend fun compatibilityReport(address: String): CompatibilityReport = lock.withLock {
        withContext(io) { CompatibilityReader(transport, parser).read(address) }
    }

    suspend fun connect(address: String) = operation(ErrorContext.CONNECTION) {
        browsers.values.forEach { it.resetPath() }
        mutableBrowser.value = BrowserState()
        ip = null
        verified = false
        refreshFailures = 0
        mutablePlayer.value = PlayerState()
        mutableStatus.value = ReceiverStatus(connectionState = ConnectionState.CONNECTING, address = address.trim())
        val parsed = ReceiverAddress.parse(address)
        mutableTuner.value = TunerState()
        val target = transport.resolve(parsed)
        val description = optional { parser.description(transport.description(target)) }
        val config = optional { parser.config(transport.command(target, YamahaCommand.Config)) }
        val model = config?.model ?: description?.model
        if (!model.equals("R-N301", ignoreCase = true))
            throw YamahaException(ReceiverError(ErrorKind.UNSUPPORTED_DEVICE, "Reported model: ${model ?: "unknown"}"))
        // A model string alone is insufficient: demand a valid Yamaha basic status envelope.
        val basic = parser.status(transport.command(target, YamahaCommand.Status))
        val inputs = if ("Main_Zone,Input,Input_Sel_Item" in (description?.paths ?: emptySet())) {
            optional { parser.inputs(transport.command(target, YamahaCommand.Inputs)) }
        } else null
        val range = listOfNotNull(description?.volumeRange, config?.volumeRange).firstOrNull {
            basic.volume == null || it.supports(basic.volume)
        }
        store.write(parsed.host)
        ip = target
        verified = true
        mutableStatus.value = basic.copy(
            connectionState = ConnectionState.CONNECTED, address = parsed.host, modelName = model,
            firmwareVersion = config?.firmware,
            volumeRange = range, inputsConfirmed = inputs != null, sources = inputs ?: Source.known.map { source ->
                source.copy(selectable = source.id != "COAXIAL" || basic.currentSource?.id == source.id)
            },
            notice = listOfNotNull(
                if (inputs == null) "Showing known R-N301 inputs; availability is not confirmed." else null,
                if (range == null) "Absolute volume limits unavailable; slider disabled." else null,
                if (basic.volume?.unit == "") "Receiver reports native volume units, not dB." else null
            ).joinToString(" ").ifBlank { null }
        )
    }

    suspend fun refresh() = operation(ErrorContext.REFRESH) {
        if (!verified) return@operation
        refreshLocked()
    }

    suspend fun power(on: Boolean) = action { YamahaCommand.Power(on) }
    suspend fun toggleWidgetPower() = action(refreshFirst = true) {
        if (status.value.powerState == PowerState.UNAVAILABLE) unsupported("Power state unknown")
        YamahaCommand.Power(status.value.powerState != PowerState.ON)
    }
    suspend fun mute(on: Boolean) = action(ErrorContext.MUTE) { YamahaCommand.Mute(on) }
    suspend fun toggleMute() = action(ErrorContext.MUTE) {
        val mute = status.value.muted ?: unsupported("Mute state unknown")
        YamahaCommand.Mute(!mute)
    }
    suspend fun selectSource(id: String) = action {
        if (status.value.sources.none { it.id == id && it.selectable }) unsupported("Input is not selectable")
        YamahaCommand.Input(id)
    }
    suspend fun setVolume(value: Int) = action { volumeCommand(value) }
    suspend fun setRotaryVolume(value: Int) = operation {
        requireConnectedOn()
        refreshLocked()
        requireConnectedOn()
        val bounds = RotaryVolumePolicy.bounds(status.value) ?: unsupported("Native rotary volume unavailable")
        if (value !in RotaryVolumePolicy.DOCUMENTED_MIN..RotaryVolumePolicy.DOCUMENTED_MAX)
            unsupported("Invalid rotary target")
        val volume = status.value.volume ?: unsupported("Volume unavailable")
        val target = value.coerceIn(bounds.min, bounds.max)
        if (target != volume.value) {
            parser.response(transport.command(ip!!, YamahaCommand.SetVolume(volume.copy(value = target))), "PUT")
            refreshLocked()
        }
    }
    /** Live targets are sampled after acquiring the mutex; acknowledgements are not receiver state. */
    suspend fun writeLiveVolume(latest: () -> Int?): LiveVolumeResult = lock.withLock {
        withContext(io) {
            try {
                requireConnectedOn()
                if (status.value.stale) unsupported("Receiver state is stale")
                val bounds = RotaryVolumePolicy.bounds(status.value) ?: unsupported("Native rotary unavailable")
                val volume = status.value.volume ?: unsupported("Volume unavailable")
                val desired = latest() ?: return@withContext LiveVolumeResult()
                if (desired !in RotaryVolumePolicy.DOCUMENTED_MIN..RotaryVolumePolicy.DOCUMENTED_MAX)
                    unsupported("Invalid live volume target")
                val target = desired.coerceIn(bounds.min, bounds.max)
                log("Live volume target=$target")
                parser.response(transport.liveVolume(ip!!, YamahaCommand.SetVolume(volume.copy(value = target))), "PUT")
                LiveVolumeResult(target)
            } catch (e: YamahaException) {
                log("Live volume failed: ${e.error.kind}")
                // Do not disconnect, cancel preview, or create a GET/PUT retry storm.
                LiveVolumeResult(error = e.error.copy(context = ErrorContext.COMMAND))
            }
        }
    }
    suspend fun reconcileLiveVolume(error: ReceiverError?) = operation {
        refreshLocked()
        mutableStatus.value = status.value.copy(error = error)
    }

    suspend fun adjustVolume(direction: Int) = operation {
        requireConnectedOn()
        // Fresh read within the same lock: never step from a stale displayed value.
        refreshLocked()
        requireConnectedOn()
        val state = status.value
        val volume = state.volume ?: unsupported("Volume unavailable")
        val range = state.volumeRange
        val target = if (range != null && range.supports(volume)) {
            if (direction !in listOf(-1, 1) || !range.accepts(volume.value)) unsupported("Invalid volume step")
            volume.copy(value = (volume.value + direction * range.step).coerceIn(range.min, range.max))
        } else NativeVolumeStep.target(volume, direction) ?: unsupported("Native volume step unavailable")
        if (target != volume) {
            parser.response(transport.command(ip!!, YamahaCommand.SetVolume(target)), "PUT")
            refreshLocked()
        }
    }

    private fun requireConnectedOn() {
        if (!verified || status.value.connectionState != ConnectionState.CONNECTED ||
            status.value.powerState != PowerState.ON) unsupported("Receiver must be connected and on")
    }

    suspend fun controlPlayer(action: PlayerAction) = operation {
        requireConnectedOn()
        refreshLocked()
        requireConnectedOn()
        val source = status.value.currentSource?.id
        if (!PlayerSources.supported(source)) unsupported("Player source is not active")
        try {
            val actual = playerApi.getNowPlaying(ip!!, source!!)
            mutablePlayer.value = PlayerState(actual)
            if (actual.availability == "Not Ready") unsupported("Player is not ready")
            playerApi.control(ip!!, source, action)
            refreshLocked()
            if (!playerVisible && PlayerSources.supported(status.value.currentSource?.id)) refreshPlayerLocked()
        } catch (e: YamahaException) {
            log("Player control: ${e.error.kind}")
            mutablePlayer.value = PlayerState(error = e.error)
        }
    }

    suspend fun refreshPlayer() = operation {
        if (verified && status.value.powerState == PowerState.ON && PlayerSources.supported(status.value.currentSource?.id))
            refreshPlayerLocked()
    }
    private suspend fun refreshPlayerLocked() {
        try {
            val info = playerApi.getNowPlaying(ip ?: return, status.value.currentSource?.id ?: return)
            log("${info.source} Play_Info state=${info.playbackState} availability=${info.availability}")
            mutablePlayer.value = PlayerState(info)
        }
        catch (e: YamahaException) {
            log("Player read: ${e.error.kind}")
            mutablePlayer.value = PlayerState(error = e.error)
        }
    }

    suspend fun openBrowser(source: String = "SERVER") = browserOperation(source = source) {
        log("Browser activation: $source")
        // Input switching is asynchronous on some firmware. Read only until confirmed.
        var active = false
        for (attempt in 0..5) {
            refreshLocked()
            if (status.value.powerState == PowerState.ON && status.value.currentSource?.id == browserApi.source) {
                active = true
                break
            }
            if (attempt < 5) delay(500)
        }
        if (!active) throw BrowserException(BrowserFailure.INACTIVE)
        browserApi.getCurrentList(ip!!)
    }
    suspend fun refreshBrowser() = browserOperation { requireBrowserActive(); browserApi.getCurrentList(ip!!) }
    suspend fun selectMedia(item: MediaItem) {
        val expected = browser.value.list ?: return
        browserOperation {
            requireBrowserActive()
            browserApi.selectItem(ip!!, expected, item).also {
                if (item.playable) refreshPlayerLocked()
            }
        }
    }
    suspend fun browserPage(next: Boolean) {
        val expected = browser.value.list ?: return
        browserOperation { requireBrowserActive(); browserApi.changePage(ip!!, expected, next) }
    }
    suspend fun browserHome() = browserOperation { requireBrowserActive(); browserApi.goHome(ip!!) }
    suspend fun browserBack(): Boolean {
        var leave = false
        browserOperation {
            requireBrowserActive()
            val current = browserApi.getCurrentWindow(ip!!)
            if (current.root) { leave = true; current } else browserApi.goBack(ip!!)
        }
        return leave
    }
    private suspend fun requireBrowserActive() {
        refreshLocked()
        if (status.value.powerState != PowerState.ON || status.value.currentSource?.id != browserApi.source)
            throw BrowserException(BrowserFailure.INACTIVE)
    }
    suspend fun navigateRadioPath(path: List<String>) = browserOperation(source = "NET RADIO", timeoutMs = 125_000) {
        requireBrowserActive()
        RadioPathNavigator(radioPages).navigatePath(ip!!, path) { requireBrowserActive() }
        refreshPlayerLocked()
        browserApi.getCurrentList(ip!!)
    }
    private suspend fun browserOperation(
        source: String = browser.value.source,
        timeoutMs: Long = 90_000,
        block: suspend () -> MediaList
    ) = lock.withLock {
        withContext(io) {
            if (source !in browsers) return@withContext
            if (browser.value.source != source) mutableBrowser.value = BrowserState(source = source)
            mutableBrowser.value = browser.value.copy(phase = BrowserPhase.LOADING, error = null)
            try {
                if (!verified || ip == null) throw BrowserException(BrowserFailure.INACTIVE)
                val list = withTimeoutOrNull(timeoutMs) { block() } ?: throw BrowserException(BrowserFailure.TIMEOUT)
                mutableBrowser.value = BrowserState(source = source, list = list,
                    navigationPath = browserApi.navigationPath, selectedStation = browserApi.selectedStation,
                    phase = if (list.items.isEmpty()) BrowserPhase.EMPTY else BrowserPhase.CONTENT)
            } catch (e: CancellationException) {
                browserApi.resetPath()
                mutableBrowser.value = browser.value.copy(phase = BrowserPhase.IDLE, list = null, navigationPath = null, selectedStation = null)
                throw e
            } catch (e: BrowserException) {
                log("$source browser error: ${e.failure}")
                browserApi.resetPath()
                mutableBrowser.value = browser.value.copy(phase = BrowserPhase.ERROR, error = e.failure, navigationPath = null, selectedStation = null)
            } catch (e: YamahaException) {
                log("Browser: ${e.error.kind} ${e.error.detail.orEmpty()}")
                val failure = when(e.error.kind) {
                    ErrorKind.TIMEOUT -> BrowserFailure.TIMEOUT
                    ErrorKind.NETWORK_UNAVAILABLE -> BrowserFailure.NETWORK
                    ErrorKind.INVALID_RESPONSE -> BrowserFailure.INVALID_RESPONSE
                    else -> BrowserFailure.LOAD_FAILED
                }
                browserApi.resetPath()
                mutableBrowser.value = browser.value.copy(phase = BrowserPhase.ERROR, error = failure, navigationPath = null, selectedStation = null)
            }
            // Browser/media-server failure is isolated; receiver reachability is assessed by status polling.
        }
    }

    suspend fun tuneFm(text: String) = manualTuner(TunerBand.FM) { range ->
        YamahaCommand.TuneFm(range.parse(text) ?: unsupported("Frequency is outside the advertised FM range or step"))
    }
    suspend fun stepFm(direction: Int) = manualTuner(TunerBand.FM) { range ->
        YamahaCommand.TuneFm(range.stepFrom(tuner.value.status?.frequency, direction)
            ?: unsupported("Frequency cannot step beyond the advertised range"))
    }
    suspend fun seekFm(up: Boolean) = manualTuner(TunerBand.FM) { YamahaCommand.SeekFm(up) }
    suspend fun tuneAm(text: String) = manualTuner(TunerBand.AM) { range ->
        YamahaCommand.TuneAm(range.parse(text) ?: unsupported("Invalid AM frequency"))
    }
    suspend fun stepAm(direction: Int) = manualTuner(TunerBand.AM) { range ->
        YamahaCommand.TuneAm(range.stepFrom(tuner.value.status?.frequency, direction)
            ?: unsupported("Invalid AM step"))
    }
    suspend fun seekAm(up: Boolean) = manualTuner(TunerBand.AM) { YamahaCommand.SeekAm(up) }
    suspend fun setBand(band: TunerBand) = tunerAction {
        if (tuner.value.range(band) == null) unsupported("Band configuration unavailable")
        YamahaCommand.SetBand(band)
    }
    suspend fun setFmMode(mode: FmMode) = tunerAction {
        if (tuner.value.status?.band != "FM") unsupported("FM reception requires FM")
        YamahaCommand.SetFmMode(mode)
    }
    private suspend fun manualTuner(band: TunerBand, command: (TuningRange) -> YamahaCommand) = tunerAction {
        if (tuner.value.status?.band != band.name) unsupported("Requested band is not active")
        command(tuner.value.range(band) ?: unsupported("Band range and step unavailable"))
    }
    private suspend fun tunerAction(command: () -> YamahaCommand) = operation {
        requireConnectedOn()
        refreshLocked()
        requireConnectedOn()
        if (status.value.currentSource?.id != "TUNER") unsupported("Tuner is not active")
        refreshTunerLocked()
        if (tuner.value.status?.availability != "Ready") unsupported("Tuner is not ready")
        parser.response(transport.command(ip!!, command()), "PUT")
        refreshLocked()
        if (!tunerVisible) refreshTunerLocked()
    }

    suspend fun refreshTuner() = operation {
        if (verified && status.value.powerState == PowerState.ON && status.value.currentSource?.id == "TUNER") {
            mutableTuner.value = tuner.value.copy(presetsLoaded = false, presetError = null, configLoaded = false)
            refreshTunerLocked()
        }
    }

    suspend fun selectPreset(number: Int) = action { presetCommand(number) }

    suspend fun stepPreset(direction: Int) = action {
        val state = tuner.value
        if (!state.presetsLoaded || state.presets.isEmpty()) unsupported("Preset list unavailable")
        val current = state.status?.preset
        val ids = state.presets.map { it.number }
        val next = if (direction > 0) ids.firstOrNull { current == null || it > current } ?: ids.first()
            else ids.lastOrNull { current == null || it < current } ?: ids.last()
        presetCommand(next)
    }

    private fun presetCommand(number: Int): YamahaCommand {
        if (status.value.currentSource?.id != "TUNER") unsupported("Select Tuner first")
        val state = tuner.value
        if (state.status?.availability != "Ready") unsupported("Tuner is not ready")
        if (state.presetsLoaded) {
            if (state.presets.none { it.number == number }) unsupported("Preset not available")
        } else if (number !in 1..8) unsupported("Unverified preset")
        return YamahaCommand.SetPreset(number)
    }

    private suspend fun refreshTunerLocked() {
        if (!tuner.value.configLoaded) {
            val config = optional { transport.command(ip!!, YamahaCommand.TunerConfig) }
            val fm = config?.let { optional { parser.tunerConfig(it, TunerBand.FM) } }
            val am = config?.let { optional { parser.tunerConfig(it, TunerBand.AM) } }
            mutableTuner.value = tuner.value.copy(fmRange = fm, amRange = am, configLoaded = true)
        }
        try {
            val info = parser.tuner(transport.command(ip ?: return, YamahaCommand.TunerInfo))
            mutableTuner.value = tuner.value.copy(status = info, error = null)
        } catch (e: YamahaException) {
            log("Tuner status: ${e.error.kind}")
            mutableTuner.value = tuner.value.copy(status = null, error = e.error)
            return
        }
        if (!tuner.value.presetsLoaded && tuner.value.presetError == null) {
            try {
                val presets = parser.tunerPresets(transport.command(ip!!, YamahaCommand.TunerPresets))
                mutableTuner.value = tuner.value.copy(presets = presets, presetsLoaded = true, presetError = null)
            } catch (e: YamahaException) {
                log("Tuner presets: ${e.error.kind}")
                mutableTuner.value = tuner.value.copy(presetError = e.error)
            }
        }
    }

    private fun volumeCommand(value: Int): YamahaCommand {
        val state = status.value
        val volume = state.volume ?: unsupported("Volume unavailable")
        val range = state.volumeRange ?: unsupported("Volume limits unavailable")
        if (!range.supports(volume) || !range.accepts(value)) unsupported("Invalid volume step or range")
        return YamahaCommand.SetVolume(volume.copy(value = value))
    }

    private suspend fun action(context: ErrorContext = ErrorContext.COMMAND, refreshFirst: Boolean = false, command: () -> YamahaCommand) = operation(context) {
        if (!verified || status.value.connectionState != ConnectionState.CONNECTED) unsupported("Receiver not connected")
        if (refreshFirst) refreshLocked()
        val cmd = command()
        if (cmd !is YamahaCommand.Power && status.value.powerState != PowerState.ON) unsupported("Receiver is in standby")
        parser.response(transport.command(ip!!, cmd), "PUT")
        if (cmd is YamahaCommand.Mute) log("Mute acknowledgement valid; reading Basic_Status")
        // Do not update optimistically from an acknowledgement; ask the receiver.
        refreshLocked()
        if (cmd is YamahaCommand.Mute) log("Mute readback: ${status.value.muted}")
    }

    private suspend fun refreshLocked() {
        val basic = parser.status(transport.command(ip ?: return, YamahaCommand.Status))
        refreshFailures = 0
        mutableStatus.value = status.value.copy(
            connectionState = ConnectionState.CONNECTED, stale = false,
            powerState = basic.powerState, volume = basic.volume, muted = basic.muted,
            currentSource = basic.currentSource,
            error = status.value.error?.takeIf { it.context == ErrorContext.COMMAND || it.context == ErrorContext.MUTE },
            volumeRange = status.value.volumeRange?.takeIf { basic.volume == null || it.supports(basic.volume) }
        )
        if (basic.powerState != PowerState.ON || basic.currentSource?.id != browserApi.source) {
            browsers.values.forEach { it.resetPath() }
            if (browser.value.phase != BrowserPhase.LOADING) mutableBrowser.value = BrowserState(source = browser.value.source)
        }
        if (basic.powerState != PowerState.ON || basic.currentSource?.id != "TUNER") {
            mutableTuner.value = TunerState()
        } else if (tunerVisible) refreshTunerLocked()
        if (player.value.nowPlaying?.source != basic.currentSource?.id) mutablePlayer.value = PlayerState()
        if (basic.powerState != PowerState.ON || !PlayerSources.supported(basic.currentSource?.id)) {
            mutablePlayer.value = PlayerState()
        } else if (playerVisible) refreshPlayerLocked()
    }

    private suspend fun <T> optional(block: suspend () -> T): T? = try { block() }
    catch (e: YamahaException) {
        log("Optional capability read: ${e.error.kind} ${e.error.detail.orEmpty()}")
        null
    }

    private suspend fun operation(context: ErrorContext = ErrorContext.COMMAND, block: suspend () -> Unit) = lock.withLock {
        withContext(io) {
            try {
                if (context == ErrorContext.COMMAND || context == ErrorContext.MUTE)
                    mutableStatus.value = status.value.copy(error = null)
                block()
            }
            catch (e: CancellationException) {
                if (status.value.connectionState == ConnectionState.CONNECTING)
                    mutableStatus.value = status.value.copy(connectionState = ConnectionState.DISCONNECTED)
                throw e
            }
            catch (e: YamahaException) {
                log("Receiver error: ${e.error.kind} ${e.error.detail.orEmpty()}")
                val error = e.error.copy(context = context)
                if ((context == ErrorContext.COMMAND || context == ErrorContext.MUTE) && verified) {
                    // One failed command cannot prove the receiver disappeared. Probe status, never retry PUT.
                    if (context == ErrorContext.MUTE || e.error.kind != ErrorKind.UNSUPPORTED_COMMAND) {
                        try { refreshLocked() }
                        catch (probe: YamahaException) {
                            log("Post-command status failed: ${probe.error.kind}")
                            mutableStatus.value = status.value.copy(stale = true)
                        }
                    }
                    mutableStatus.value = status.value.copy(error = error)
                } else {
                    refreshFailures++
                    val unavailable = context == ErrorContext.CONNECTION || refreshFailures >= 3
                    mutableStatus.value = status.value.copy(
                        connectionState = if (unavailable) ConnectionState.UNAVAILABLE else status.value.connectionState,
                        powerState = if (unavailable) PowerState.UNAVAILABLE else status.value.powerState,
                        volume = if (unavailable) null else status.value.volume,
                        muted = if (unavailable) null else status.value.muted,
                        currentSource = if (unavailable) null else status.value.currentSource,
                        stale = true, error = error
                    )
                    if (unavailable) { mutableTuner.value = TunerState(); mutablePlayer.value = PlayerState(); mutableBrowser.value = BrowserState() }
                }
            }
        }
    }
    private fun unsupported(detail: String): Nothing =
        throw YamahaException(ReceiverError(ErrorKind.UNSUPPORTED_COMMAND, detail))
}
