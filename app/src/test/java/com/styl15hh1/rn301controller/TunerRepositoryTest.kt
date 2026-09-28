package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TunerRepositoryTest {
    private class Fake : YamahaTransport {
        val commands = mutableListOf<YamahaCommand>()
        var source = "TUNER"
        var preset = 6
        var tunerFails = false
        var presetFails = false
        override suspend fun resolve(address: ReceiverAddress) = "192.168.1.55"
        override suspend fun description(ip: String) = "<Unit_Description Unit_Name=\"R-N301\"/>"
        override suspend fun command(ip: String, command: YamahaCommand): String {
            commands.add(command)
            return when (command) {
                YamahaCommand.Config -> configXml
                YamahaCommand.Status -> statusXml(source = source)
                YamahaCommand.TunerInfo -> {
                    if (tunerFails) throw YamahaException(ReceiverError(ErrorKind.TIMEOUT))
                    fixture("rn301-tuner-not-ready.xml").replace("Not Ready", "Ready")
                        .replace("<Preset_Sel>1</Preset_Sel>", "<Preset_Sel>$preset</Preset_Sel>")
                }
                YamahaCommand.TunerPresets -> {
                    if (presetFails) throw YamahaException(ReceiverError(ErrorKind.UNSUPPORTED_COMMAND))
                    fixture("rn301-presets.xml")
                }
                is YamahaCommand.SetPreset -> {
                    preset = command.number
                    "<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>"
                }
                else -> "<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>"
            }
        }
    }
    private val store = object : AddressStore {
        override suspend fun read() = ""
        override suspend fun write(address: String) = Unit
    }
    @Test fun nextPreviousUseReturnedSixPresetsAndRefresh() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        repo.showTuner(true)
        repo.refresh()
        repo.stepPreset(1)
        assertEquals(1, repo.tuner.value.status!!.preset)
        repo.stepPreset(-1)
        assertEquals(6, repo.tuner.value.status!!.preset)
        assertTrue(fake.commands.contains(YamahaCommand.SetPreset(6)))
        assertEquals(YamahaCommand.TunerInfo, fake.commands.last())
    }
    @Test fun tunerFailureDoesNotEraseWorkingPowerStatus() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        repo.showTuner(true)
        fake.tunerFails = true
        repo.refresh()
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
        assertEquals(PowerState.ON, repo.status.value.powerState)
        assertEquals(ErrorKind.TIMEOUT, repo.tuner.value.error!!.kind)
        assertNull(repo.tuner.value.status)
    }
    @Test fun sourceChangeClearsMetadataAndStopsTunerRequests() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        repo.showTuner(true)
        repo.refresh()
        fake.source = "Spotify"
        fake.commands.clear()
        repo.refresh()
        assertEquals(listOf(YamahaCommand.Status), fake.commands)
        assertNull(repo.tuner.value.status)
        assertTrue(repo.tuner.value.presets.isEmpty())
    }
    @Test fun hiddenTunerDoesNotPollMetadata() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        fake.commands.clear()
        repo.refresh()
        assertEquals(listOf(YamahaCommand.Status), fake.commands)
    }
    @Test fun presetListFailureDoesNotInventEightStoredStations() = runTest {
        val fake = Fake().apply { presetFails = true }
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        repo.showTuner(true)
        repo.refresh()
        assertFalse(repo.tuner.value.presetsLoaded)
        assertTrue(repo.tuner.value.presets.isEmpty())
        val writes = fake.commands.count { it is YamahaCommand.SetPreset }
        repo.stepPreset(1)
        assertEquals(writes, fake.commands.count { it is YamahaCommand.SetPreset })
    }
    @Test fun unadvertisedPresetIsNotSent() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        repo.showTuner(true)
        repo.refresh()
        repo.selectPreset(8)
        assertFalse(fake.commands.contains(YamahaCommand.SetPreset(8)))
    }
}
