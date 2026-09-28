package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.player.*
import com.styl15hh1.rn301controller.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import com.styl15hh1.rn301controller.ui.ReceiverViewModel
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V03RepositoryTest {
    private class Fake : YamahaTransport {
        val commands = mutableListOf<YamahaCommand>()
        var source = "Spotify"
        var volume = 40
        var fm = 9740
        var playback = "Pause"
        var playerFails = false
        var ackOnly = false
        var unavailable = false
        private fun fixture(name: String) = javaClass.getResource("/$name")!!.readText()
        override suspend fun resolve(address: ReceiverAddress) = "192.168.1.55"
        override suspend fun description(ip: String) = "<Unit_Description Unit_Name=\"R-N301\"/>"
        override suspend fun command(ip: String, command: YamahaCommand): String {
            commands += command
            if (unavailable) throw YamahaException(ReceiverError(ErrorKind.TIMEOUT))
            return when (command) {
                YamahaCommand.Config -> "<YAMAHA_AV rsp=\"GET\" RC=\"0\"><System><Config><Model_Name>R-N301</Model_Name></Config></System></YAMAHA_AV>"
                YamahaCommand.Status -> "<YAMAHA_AV rsp=\"GET\" RC=\"0\"><Main_Zone><Basic_Status><Power_Control><Power>On</Power></Power_Control><Volume><Lvl><Val>$volume</Val><Exp>0</Exp><Unit/></Lvl><Mute>Off</Mute></Volume><Input><Input_Sel>$source</Input_Sel></Input></Basic_Status></Main_Zone></YAMAHA_AV>"
                YamahaCommand.TunerConfig -> fixture("rn301-tuner-config.xml")
                YamahaCommand.TunerPresets -> fixture("rn301-presets.xml")
                YamahaCommand.TunerInfo -> fixture("rn301-tuner-not-ready.xml").replace("Not Ready", "Ready").replace("9740", fm.toString())
                YamahaCommand.SpotifyInfo -> {
                    if (playerFails) throw YamahaException(ReceiverError(ErrorKind.INVALID_RESPONSE))
                    fixture("rn301-spotify-not-ready.xml").replace("Not Ready", "Ready").replace("<Playback_Info>Stop", "<Playback_Info>$playback")
                }
                is YamahaCommand.SetVolume -> { if (!ackOnly) volume = command.volume.value; ack }
                is YamahaCommand.TuneFm -> { if (!ackOnly) fm = command.value; ack }
                is YamahaCommand.SpotifyControl -> { if (!ackOnly) playback = command.action.wire; ack }
                else -> ack
            }
        }
        private val ack = "<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>"
    }
    private val store = object : AddressStore {
        override suspend fun read() = ""
        override suspend fun write(address: String) = Unit
    }
    private suspend fun TestScope.repo(fake: Fake) = YamahaRepository(fake, store,
        io = StandardTestDispatcher(testScheduler)).also { it.connect("receiver") }

    @Test fun nativeTapReadsFreshAndWritesExactlyOneStepWithoutLimits() = runTest {
        val fake = Fake()
        val repo = repo(fake)
        fake.volume = 44 // Changed by the physical remote after the UI's last read.
        fake.commands.clear()
        repo.adjustVolume(1)
        assertEquals(listOf(YamahaCommand.Status, YamahaCommand.SetVolume(Volume(45, 0, "")), YamahaCommand.Status), fake.commands)
        assertEquals(45, repo.status.value.volume!!.value)
        repo.adjustVolume(-1)
        assertEquals(44, repo.status.value.volume!!.value)
    }
    @Test fun volumeAckDoesNotInventNewDisplayValue() = runTest {
        val fake = Fake().apply { ackOnly = true }
        val repo = repo(fake)
        repo.adjustVolume(1)
        assertEquals(40, repo.status.value.volume!!.value)
    }
    @Test fun volumeTimeoutDoesNotWriteStaleValueOrRetry() = runTest {
        val fake = Fake()
        val repo = repo(fake)
        fake.commands.clear(); fake.unavailable = true
        repo.adjustVolume(1)
        assertEquals(listOf(YamahaCommand.Status, YamahaCommand.Status), fake.commands)
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
        assertTrue(repo.status.value.stale)
        assertEquals(ErrorKind.TIMEOUT, repo.status.value.error!!.kind)
    }
    @Test fun manualFmUsesAdvertisedStepAndFreshFrequency() = runTest {
        val fake = Fake().apply { source = "TUNER" }
        val repo = repo(fake)
        repo.showTuner(true); repo.refresh()
        fake.fm = 10160
        fake.commands.clear()
        repo.stepFm(1)
        assertEquals(listOf(YamahaCommand.TuneFm(10165)), fake.commands.filterIsInstance<YamahaCommand.TuneFm>())
        assertEquals(10165, repo.tuner.value.status!!.frequency!!.value)
    }
    @Test fun invalidDirectFrequencyNeverWrites() = runTest {
        val fake = Fake().apply { source = "TUNER" }
        val repo = repo(fake)
        repo.tuneFm("101.63")
        assertTrue(fake.commands.none { it is YamahaCommand.TuneFm })
        assertEquals(ErrorKind.UNSUPPORTED_COMMAND, repo.status.value.error!!.kind)
    }
    @Test fun seekWritesOnceAndRetainsPresetList() = runTest {
        val fake = Fake().apply { source = "TUNER" }
        val repo = repo(fake)
        repo.showTuner(true); repo.refresh()
        repo.seekFm(true)
        assertEquals(1, fake.commands.count { it == YamahaCommand.SeekFm(true) })
        assertEquals(6, repo.tuner.value.presets.size)
    }
    @Test fun playerAckDoesNotOptimisticallyChangePause() = runTest {
        val fake = Fake().apply { ackOnly = true }
        val repo = repo(fake)
        repo.showPlayer(true); repo.refresh()
        repo.controlPlayer(PlayerAction.PLAY)
        assertEquals(PlaybackState.PAUSED, repo.player.value.nowPlaying!!.playback)
        assertEquals(1, fake.commands.count { it == YamahaCommand.SpotifyControl(PlayerAction.PLAY) })
    }
    @Test fun malformedPlayerDoesNotInvalidateBasicReceiverConnection() = runTest {
        val fake = Fake().apply { playerFails = true }
        val repo = repo(fake)
        repo.showPlayer(true); repo.refresh()
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
        assertNull(repo.player.value.nowPlaying)
        assertEquals(ErrorKind.INVALID_RESPONSE, repo.player.value.error!!.kind)
    }
    @Test fun switchingSourceClearsPlayerAndStopsPlayerReads() = runTest {
        val fake = Fake()
        val repo = repo(fake)
        repo.showPlayer(true); repo.refresh()
        fake.source = "CD"; fake.commands.clear()
        repo.refresh()
        assertNull(repo.player.value.nowPlaying)
        assertEquals(listOf(YamahaCommand.Status), fake.commands)
    }
    @Test fun hiddenPlayerDoesNotPoll() = runTest {
        val fake = Fake()
        val repo = repo(fake)
        repo.showPlayer(false); fake.commands.clear(); repo.refresh()
        assertEquals(listOf(YamahaCommand.Status), fake.commands)
    }
    @Test fun playerChecksSourceAgainBeforeSendingAction() = runTest {
        val fake = Fake()
        val repo = repo(fake)
        fake.source = "CD"; fake.commands.clear()
        repo.controlPlayer(PlayerAction.NEXT)
        assertTrue(fake.commands.none { it is YamahaCommand.SpotifyControl })
    }
    @Test fun spotifyPollingTracksVisiblePageAndStopsInBackground() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val fake = Fake()
        val repo = repo(fake)
        val vm = ReceiverViewModel(repo)
        try {
            runCurrent()
            vm.foreground(true); runCurrent()
            val first = fake.commands.count { it == YamahaCommand.SpotifyInfo }
            assertEquals(1, first)
            advanceTimeBy(2001); runCurrent()
            assertEquals(first + 1, fake.commands.count { it == YamahaCommand.SpotifyInfo })
            vm.settings()
            advanceTimeBy(4001); runCurrent()
            assertEquals(first + 1, fake.commands.count { it == YamahaCommand.SpotifyInfo })
            vm.home()
            advanceTimeBy(2001); runCurrent()
            val beforeBackground = fake.commands.size
            vm.foreground(false)
            advanceTimeBy(20000); runCurrent()
            assertEquals(beforeBackground, fake.commands.size)
        } finally { vm.foreground(false); Dispatchers.resetMain() }
    }

    @Test fun genericPlayerDoesNotExposeRadioControls() = runTest {
        val fake = Fake()
        val player = LegacyYamahaPlayer(fake, YamahaXmlParser())
        try {
            player.control("192.168.1.55", "NET RADIO", PlayerAction.PLAY)
            fail("Unsupported source must be rejected")
        } catch (expected: YamahaException) { assertEquals(ErrorKind.UNSUPPORTED_COMMAND, expected.error.kind) }
        assertTrue(fake.commands.isEmpty())
    }
}
