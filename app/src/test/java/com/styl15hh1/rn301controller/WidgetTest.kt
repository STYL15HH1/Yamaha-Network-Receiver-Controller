package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.widget.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WidgetTest {
    private val on = ReceiverStatus(connectionState = ConnectionState.CONNECTED, powerState = PowerState.ON,
        modelName = "R-N301", currentSource = Source("TUNER"))
    private fun present(status: ReceiverStatus = on, tuner: TunerState = TunerState(), player: PlayerState = PlayerState(), settings: SettingsState = SettingsState()) =
        WidgetPresentation.from(status, tuner, player, settings)
    @Test fun usesExistingDefaultFavoritesAndOrder() {
        assertEquals(listOf("OPTICAL", "TUNER", "Spotify"), present().favorites.map { it.id })
    }
    @Test fun configuredEmptyFavoritesStayEmpty() {
        assertTrue(present(settings = SettingsState(quickSourcesConfigured = true)).favorites.isEmpty())
    }
    @Test fun unavailableSourcesAreFilteredAndFewerFavoritesAreValid() {
        val status = on.copy(sources = listOf(Source("SERVER"), Source("TUNER", selectable = false)))
        assertEquals(listOf("SERVER"), present(status, settings = SettingsState(quickSources = listOf("TUNER", "SERVER", "missing"), quickSourcesConfigured = true)).favorites.map { it.id })
    }
    @Test fun powerAndOfflineMappingUseActualState() {
        assertEquals(PowerState.ON, present().power); assertFalse(present().offline)
        assertEquals(PowerState.STANDBY, present(on.copy(powerState = PowerState.STANDBY)).power)
        assertEquals(PowerState.UNAVAILABLE, present(ReceiverStatus()).power)
        assertTrue(present(ReceiverStatus()).offline)
        assertTrue(present(on.copy(stale = true)).offline)
        assertTrue(present(on.copy(error = ReceiverError(ErrorKind.TIMEOUT))).offline)
    }
    @Test fun tunerUsesOnlyRdsStationAndHidesItInStandby() {
        val tuner = TunerState(status = TunerStatus(nowPlaying = NowPlaying("TUNER", station = "TOK FM", title = "Radio text")))
        assertEquals("TOK FM", present(tuner = tuner).metadata)
        assertNull(present(on.copy(powerState = PowerState.STANDBY), tuner).metadata)
        assertNull(present(tuner = tuner.copy(error = ReceiverError(ErrorKind.TIMEOUT))).metadata)
    }
    @Test fun playerMetadataMustMatchCurrentSource() {
        val player = PlayerState(NowPlaying("Spotify", title = "Song"))
        assertNull(present(on.copy(currentSource = Source("SERVER")), player = player).metadata)
        assertEquals("Song", present(on.copy(currentSource = Source("Spotify")), player = player).metadata)
        assertNull(present(on.copy(currentSource = Source("Spotify")), player = player.copy(error = ReceiverError(ErrorKind.TIMEOUT))).metadata)
    }
    @Test fun serverUsesTitleAndNetRadioPrefersStation() {
        for (source in listOf("SERVER", "NET RADIO")) {
            val player = PlayerState(NowPlaying(source, title = "Song", station = "Station"))
            assertEquals(if (source == "SERVER") "Song" else "Station", present(on.copy(currentSource = Source(source)), player = player).metadata)
        }
    }
    @Test fun missingMetadataAndNonMediaSourcesAreSafe() {
        assertNull(present().metadata)
        assertNull(present(on.copy(currentSource = Source("OPTICAL")), player = PlayerState(NowPlaying("SERVER", title = "Old"))).metadata)
        assertNull(present(tuner = TunerState(status = TunerStatus(nowPlaying = NowPlaying("TUNER", station = " ")))).metadata)
    }
    private class Store(var address: String = "receiver") : AddressStore {
        override suspend fun read() = address
        override suspend fun write(address: String) { this.address = address }
    }
    private class Fake : YamahaTransport {
        val commands = mutableListOf<YamahaCommand>()
        var on = false
        var source = "TUNER"
        var offline = false
        override suspend fun resolve(address: ReceiverAddress) = "192.168.1.55"
        override suspend fun description(ip: String) = descriptionXml
        override suspend fun command(ip: String, command: YamahaCommand): String {
            commands.add(command)
            if (offline) throw YamahaException(ReceiverError(ErrorKind.TIMEOUT))
            return when(command) {
                YamahaCommand.Config -> configXml
                YamahaCommand.Status -> statusXml(if (on) "On" else "Standby", source = source)
                is YamahaCommand.Power -> { on = command.on; """<YAMAHA_AV rsp="PUT" RC="0"/>""" }
                is YamahaCommand.Input -> { source = command.id; """<YAMAHA_AV rsp="PUT" RC="0"/>""" }
                else -> error("Unexpected $command")
            }
        }
    }
    @Test fun powerActionReusesVerifiedCommandAndReadsBackBothDirections() = runTest {
        val fake = Fake(); val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        val settings = AppSettings(MemorySettingsStore())
        WidgetActions.execute(repo, settings, WidgetAction.POWER)
        assertEquals(PowerState.ON, repo.status.value.powerState)
        WidgetActions.execute(repo, settings, WidgetAction.POWER)
        assertEquals(PowerState.STANDBY, repo.status.value.powerState)
        assertEquals(listOf(YamahaCommand.Power(true), YamahaCommand.Power(false)), fake.commands.filterIsInstance<YamahaCommand.Power>())
        assertEquals(YamahaCommand.Status, fake.commands.last())
    }
    @Test fun sourceActionRevalidatesFavoriteAndReadback() = runTest {
        val fake = Fake().apply { on = true }; val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        val settings = AppSettings(MemorySettingsStore()).apply { quickSources(listOf("SERVER")) }
        WidgetActions.execute(repo, settings, WidgetAction.SOURCE, "SERVER")
        assertEquals("SERVER", repo.status.value.currentSource?.id)
        WidgetActions.execute(repo, settings, WidgetAction.SOURCE, "OPTICAL")
        settings.quickSources(emptyList())
        WidgetActions.execute(repo, settings, WidgetAction.SOURCE, "SERVER")
        assertEquals(1, fake.commands.filterIsInstance<YamahaCommand.Input>().size)
    }
    @Test fun offlineAndMissingSavedReceiverDoNotSendControlCommands() = runTest {
        val fake = Fake().apply { offline = true }; val store = Store("")
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        val settings = AppSettings(MemorySettingsStore())
        WidgetActions.execute(repo, settings, WidgetAction.POWER)
        assertTrue(fake.commands.isEmpty())
        store.address = "receiver"
        WidgetActions.execute(repo, settings, WidgetAction.POWER)
        assertTrue(fake.commands.none { it is YamahaCommand.Power })
    }
    @Test fun refreshActionIsGetOnlyAndDoesNotFetchMetadata() = runTest {
        val fake = Fake(); val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        WidgetActions.execute(repo, AppSettings(MemorySettingsStore()), WidgetAction.REFRESH)
        assertEquals(listOf(YamahaCommand.Config, YamahaCommand.Status), fake.commands)
    }
    @Test fun presentationCapsMoreThanFourFavoritesWithoutMutatingSettings() {
        val ids = listOf("OPTICAL", "TUNER", "Spotify", "NET RADIO", "SERVER")
        val settings = SettingsState(quickSources = ids, quickSourcesConfigured = true)
        assertEquals(ids.take(4), present(settings = settings).favorites.map { it.id })
        assertEquals(ids, settings.quickSources)
    }

}
