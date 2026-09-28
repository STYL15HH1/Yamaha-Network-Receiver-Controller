package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.ui.ControlPolicy
import com.styl15hh1.rn301controller.ui.ReceiverViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V04Test {
    private class Fake : YamahaTransport {
        val commands = mutableListOf<YamahaCommand>()
        var muted = false
        var volume = 50
        var muteError: ErrorKind? = null
        var staleAfterWrite = false
        var wrote = false
        var advertise = false
        override suspend fun resolve(address: ReceiverAddress) = "192.168.1.55"
        override suspend fun description(ip: String) = "<Unit_Description Unit_Name=\"R-N301\"/>"
        override suspend fun command(ip: String, command: YamahaCommand): String {
            commands += command
            return when (command) {
                YamahaCommand.Config -> "<YAMAHA_AV rsp=\"GET\" RC=\"0\"><System><Config><Model_Name>R-N301</Model_Name>" +
                    (if (advertise) "<Volume><Min>10</Min><Max>80</Max><Step>1</Step></Volume>" else "") +
                    "</Config></System></YAMAHA_AV>"
                YamahaCommand.Status -> {
                    if (wrote && staleAfterWrite) throw YamahaException(ReceiverError(ErrorKind.TIMEOUT))
                    statusXml().replace("<Val>25</Val>", "<Val>$volume</Val>")
                        .replace("<Mute>Off</Mute>", "<Mute>${if (muted) "On" else "Off"}</Mute>")
                }
                is YamahaCommand.Mute -> {
                    muteError?.let { throw YamahaException(ReceiverError(it, "HTTP 400")) }
                    muted = command.on; wrote = true
                    "<YAMAHA_AV rsp=\"PUT\" RC=\"0\"><Main_Zone><Volume><Mute/></Volume></Main_Zone></YAMAHA_AV>"
                }
                is YamahaCommand.SetVolume -> { volume = command.volume.value; wrote = true; ack }
                else -> ack
            }
        }
        private val ack = "<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>"
    }
    private val addressStore = object : AddressStore {
        override suspend fun read() = ""
        override suspend fun write(address: String) = Unit
    }
    private suspend fun TestScope.repo(fake: Fake) = YamahaRepository(fake, addressStore,
        io = StandardTestDispatcher(testScheduler)).also { it.connect("receiver") }

    @Test fun muteOnAndOffUseMainZone() {
        for (on in listOf(true, false)) assertEquals(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><Main_Zone><Volume><Mute>${if(on) "On" else "Off"}</Mute></Volume></Main_Zone></YAMAHA_AV>",
            YamahaXmlBuilder.build(YamahaCommand.Mute(on)))
    }
    @Test fun emptyEchoAndBareAcknowledgementsAreValid() {
        val parser = YamahaXmlParser()
        parser.response("<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>", "PUT")
        parser.response("<YAMAHA_AV rsp=\"PUT\" RC=\"0\"><Main_Zone><Volume><Mute/></Volume></Main_Zone></YAMAHA_AV>", "PUT")
        assertThrows(YamahaException::class.java) { parser.response("<YAMAHA_AV rsp=\"PUT\" RC=\"4\"/>", "PUT") }
    }
    @Test fun successfulMuteAndUnmuteStayConnectedAndReadActualState() = runTest {
        val fake = Fake(); val repo = repo(fake)
        repo.mute(true)
        assertEquals(true, repo.status.value.muted)
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
        assertEquals(YamahaCommand.Status, fake.commands.last())
        repo.toggleMute()
        assertEquals(false, repo.status.value.muted)
        assertNull(repo.status.value.error)
    }
    @Test fun commandErrorDoesNotSlowHealthyPolling() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var vm: ReceiverViewModel? = null
        try {
            val fake = Fake().apply { muteError = ErrorKind.COMMAND_FAILED }
            val repo = repo(fake)
            repo.mute(true)
            vm = ReceiverViewModel(repo)
            runCurrent()
            fake.commands.clear()
            vm.foreground(true)
            runCurrent()
            assertEquals(1, fake.commands.count { it == YamahaCommand.Status })
            advanceTimeBy(2001)
            runCurrent()
            assertEquals(2, fake.commands.count { it == YamahaCommand.Status })
            assertEquals(ErrorContext.MUTE, repo.status.value.error!!.context)
        } finally {
            vm?.foreground(false)
            Dispatchers.resetMain()
        }
    }
    @Test fun httpCommandRejectionIsNotReceiverNotFound() {
        assertEquals(ErrorKind.COMMAND_FAILED, HttpFailure.kind(400))
        assertEquals(ErrorKind.COMMAND_FAILED, HttpFailure.kind(500))
        assertEquals(ErrorKind.UNSUPPORTED_COMMAND, HttpFailure.kind(404))
    }
    @Test fun failedMuteChecksConnectionAndDoesNotRetryPut() = runTest {
        val fake = Fake().apply { muteError = ErrorKind.COMMAND_FAILED }
        val repo = repo(fake); fake.commands.clear()
        repo.mute(true)
        assertEquals(listOf(YamahaCommand.Mute(true), YamahaCommand.Status), fake.commands)
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
        assertEquals(false, repo.status.value.muted)
        assertEquals(ErrorContext.MUTE, repo.status.value.error!!.context)
        assertEquals(ErrorKind.COMMAND_FAILED, repo.status.value.error!!.kind)
        assertFalse(repo.status.value.stale)
    }
    @Test fun successfulPutWithFailedRefreshIsNotAutomaticDisconnect() = runTest {
        val fake = Fake().apply { staleAfterWrite = true }
        val repo = repo(fake)
        repo.mute(true)
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
        assertTrue(repo.status.value.stale)
        assertFalse(ControlPolicy.powered(repo.status.value, false))
        assertEquals(1, fake.commands.count { it is YamahaCommand.Mute })
        assertEquals(ErrorContext.MUTE, repo.status.value.error!!.context)
    }
    @Test fun unavailableRangeNeverInventsSliderBounds() = runTest {
        val repo = repo(Fake())
        assertNull(repo.status.value.volumeRange)
        assertNull(ControlPolicy.sliderRange(repo.status.value))
    }
    @Test fun nativeRangeParsingAndFinalValueWrite() = runTest {
        val fake = Fake().apply { advertise = true }
        val repo = repo(fake)
        val range = ControlPolicy.sliderRange(repo.status.value)!!
        assertEquals(VolumeRange(10,80,1,0,""), range)
        assertEquals(62, ControlPolicy.sliderValue(61.7f, range))
        fake.commands.clear()
        repo.setVolume(ControlPolicy.sliderValue(61.7f, range))
        assertEquals(1, fake.commands.count { it is YamahaCommand.SetVolume })
        assertEquals(62, repo.status.value.volume!!.value)
    }
    @Test fun malformedAndMismatchedRangeCannotEnableSlider() {
        val parser = YamahaXmlParser()
        assertNull(parser.config("<YAMAHA_AV rsp=\"GET\" RC=\"0\"><System><Config><Volume><Min>80</Min><Max>10</Max><Step>1</Step></Volume></Config></System></YAMAHA_AV>").volumeRange)
        assertNull(ControlPolicy.sliderRange(ReceiverStatus(volume=Volume(40,0,""), volumeRange=VolumeRange(-800,100,5,1,"dB"))))
        assertNull(ControlPolicy.sliderRange(ReceiverStatus(volume=Volume(40,0,""), volumeRange=VolumeRange(0,100,2,0,""))))
    }
    @Test fun disabledBitmapAndOtherContentUseSameAlpha() {
        assertEquals(0.38f, ControlPolicy.contentAlpha(false), 0f)
        assertEquals(1f, ControlPolicy.contentAlpha(true), 0f)
    }
    @Test fun disconnectedStandbyBusyAndStaleControlsDisable() {
        val on = ReceiverStatus(connectionState=ConnectionState.CONNECTED,powerState=PowerState.ON)
        assertTrue(ControlPolicy.powered(on,false))
        assertFalse(ControlPolicy.powered(on,true))
        assertFalse(ControlPolicy.powered(on.copy(stale=true),false))
        assertFalse(ControlPolicy.powered(on.copy(connectionState=ConnectionState.UNAVAILABLE),false))
        assertFalse(ControlPolicy.powered(on.copy(powerState=PowerState.STANDBY),false))
    }
    private class MemorySettings : SettingsStore {
        val values = mutableMapOf<String,String>()
        override fun read(key: String) = values[key]
        override fun write(key: String, value: String) { values[key] = value }
    }
    @Test fun languageAndThemePersistAcrossSettingsInstances() {
        val store = MemorySettings()
        val first = AppSettings(store)
        assertEquals("en", first.state.value.language)
        first.language("pl"); first.appearance(Appearance.DARK)
        val second = AppSettings(store)
        assertEquals("pl", second.state.value.language)
        assertEquals(Appearance.DARK, second.state.value.appearance)
    }
    @Test fun allEightLocaleSelectionsAndFallback() {
        val settings = AppSettings(MemorySettings())
        for (tag in AppLanguages.tags) { settings.language(tag); assertEquals(tag, settings.state.value.language) }
        assertEquals("ja", AppLanguages.supported("ja-JP"))
        assertEquals("en", AppLanguages.supported("xx"))
        assertEquals(8, AppLanguages.tags.size)
    }
    @Test fun aboutUsesSuppliedBuildVersionAndAuthorLink() {
        val about = AboutInfo(BuildConfig.VERSION_NAME)
        assertEquals(BuildConfig.VERSION_NAME, about.version)
        assertEquals("STYL15HH1", about.author)
        assertEquals("https://github.com/STYL15HH1", about.github)
    }
}
