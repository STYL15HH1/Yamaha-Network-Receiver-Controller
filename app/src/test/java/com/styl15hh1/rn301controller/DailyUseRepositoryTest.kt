package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyUseRepositoryTest {
    private class Fake:YamahaTransport {
        val commands=mutableListOf<YamahaCommand>()
        var value=40
        var power="On"
        var source="CD"
        var max=99
        var failWrite=false
        var failRead=false
        override suspend fun resolve(address:ReceiverAddress)="192.168.1.55"
        override suspend fun description(ip:String)="""<Unit_Description Unit_Name="R-N301"/>"""
        override suspend fun command(ip:String,command:YamahaCommand):String {
            commands+=command
            return when(command){
                YamahaCommand.Config->configXml
                YamahaCommand.Status->{
                    if(failRead)throw YamahaException(ReceiverError(ErrorKind.TIMEOUT))
                    statusXml(power=power,value=value,source=source)
                }
                is YamahaCommand.Power->{power=if(command.on)"On" else "Standby";ack}
                is YamahaCommand.SetVolume->{
                    if(failWrite)throw YamahaException(ReceiverError(ErrorKind.COMMAND_FAILED))
                    value=command.volume.value.coerceAtMost(max);ack
                }
                is YamahaCommand.Input->{
                    if(failWrite)throw YamahaException(ReceiverError(ErrorKind.COMMAND_FAILED))
                    source=command.id;ack
                }
                else->ack
            }
        }
        private val ack="""<YAMAHA_AV rsp="PUT" RC="0"/>"""
    }
    private suspend fun TestScope.repo(f:Fake):YamahaRepository {
        val store=object:AddressStore{override suspend fun read()="";override suspend fun write(address:String)=Unit}
        return YamahaRepository(f,store,io=StandardTestDispatcher(testScheduler)).also{it.connect("receiver")}
    }
    @Test fun finalNativeTargetUsesExistingEncodingAndReadback()=runTest{
        val f=Fake();val r=repo(f);f.commands.clear();r.setRotaryVolume(55)
        assertEquals(listOf(YamahaCommand.Status,YamahaCommand.SetVolume(Volume(55,0,"")),YamahaCommand.Status),f.commands)
        assertEquals(55,r.status.value.volume!!.value)
    }
    @Test fun configuredReceiverClampingWinsReadback()=runTest{
        val f=Fake().apply{max=45};val r=repo(f);r.setRotaryVolume(55)
        assertEquals(45,r.status.value.volume!!.value)
    }
    @Test fun failedFinalWriteRefreshesWithoutDisconnectOrRetry()=runTest{
        val f=Fake();val r=repo(f);f.failWrite=true;f.commands.clear();r.setRotaryVolume(55)
        assertEquals(1,f.commands.filterIsInstance<YamahaCommand.SetVolume>().size)
        assertEquals(40,r.status.value.volume!!.value)
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
        assertEquals(ErrorContext.COMMAND,r.status.value.error!!.context)
        assertEquals(YamahaCommand.Status,f.commands.last())
    }
    @Test fun failedFreshReadNeverWritesTarget()=runTest{
        val f=Fake();val r=repo(f);f.failRead=true;f.commands.clear();r.setRotaryVolume(55)
        assertTrue(f.commands.none{it is YamahaCommand.SetVolume});assertTrue(r.status.value.stale)
    }
    @Test fun outOfGuardrailTargetsNeverWrite()=runTest{
        val f=Fake();val r=repo(f)
        for(value in listOf(0,100,-1)){r.setRotaryVolume(value)}
        assertTrue(f.commands.none{it is YamahaCommand.SetVolume})
    }
    @Test fun rapidGestureWritesLiveAndSuppressesDuplicateFinalWrite()=runTest{
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val vm=ReceiverViewModel(r)
        try{
            runCurrent();assertEquals(40,vm.rotaryState.value.value);assertTrue(vm.beginRotation(0f))
            f.commands.clear();for(angle in 1..120)vm.rotateVolume(angle.toFloat())
            advanceTimeBy(150);runCurrent()
            assertEquals(50,vm.rotaryState.value.value)
            assertTrue(vm.rotaryState.value.active)
            assertEquals(1,f.commands.filterIsInstance<YamahaCommand.SetVolume>().size)
            vm.finishRotation();assertTrue(vm.rotaryState.value.pending);advanceUntilIdle()
            assertEquals(1,f.commands.filterIsInstance<YamahaCommand.SetVolume>().size)
            assertEquals(50,vm.rotaryState.value.value);assertFalse(vm.rotaryState.value.pending)
        }finally{vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun liveFailureKeepsPreviewAndReleaseRecoversWithoutDisconnect()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val vm=ReceiverViewModel(r)
        try {
            runCurrent();vm.beginRotation(0f);f.failWrite=true;vm.rotateVolume(120f)
            advanceTimeBy(125);runCurrent()
            assertTrue(vm.rotaryState.value.active);assertEquals(50,vm.rotaryState.value.value)
            assertNull(r.status.value.error);assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
            f.failWrite=false;vm.rotateVolume(180f);vm.finishRotation();advanceUntilIdle()
            assertEquals(55,vm.rotaryState.value.value);assertNull(r.status.value.error)
            assertEquals(listOf(50,55),f.commands.filterIsInstance<YamahaCommand.SetVolume>().map{it.volume.value})
        } finally {vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun finalReadbackResolvesExternalChangeWithoutDuplicateWrite()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val vm=ReceiverViewModel(r)
        try {
            runCurrent();vm.beginRotation(0f);vm.rotateVolume(120f);advanceTimeBy(125);runCurrent()
            f.value=42;r.refresh();runCurrent();assertEquals(50,vm.rotaryState.value.value)
            vm.finishRotation();advanceUntilIdle()
            assertEquals(42,vm.rotaryState.value.value)
            assertEquals(1,f.commands.filterIsInstance<YamahaCommand.SetVolume>().size)
        } finally {vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun failedGestureRestoresActualValueInViewModel()=runTest{
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val vm=ReceiverViewModel(r)
        try{
            runCurrent();vm.beginRotation(0f);vm.rotateVolume(60f);f.failWrite=true
            vm.finishRotation();advanceUntilIdle()
            assertEquals(40,vm.rotaryState.value.value);assertFalse(vm.rotaryState.value.pending)
            assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
        }finally{vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun externalChangesDuringGestureDoNotOverwritePreview()=runTest{
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val vm=ReceiverViewModel(r)
        try{
            runCurrent();vm.beginRotation(0f);vm.rotateVolume(60f)
            f.value=38;r.refresh();runCurrent();assertEquals(45,vm.rotaryState.value.value)
            vm.finishRotation();advanceUntilIdle();assertEquals(45,vm.rotaryState.value.value)
            f.value=42;r.refresh();runCurrent();assertEquals(42,vm.rotaryState.value.value)
        }finally{vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun backgroundCancelsPreviewWithoutSendingVolume()=runTest{
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val vm=ReceiverViewModel(r)
        try{
            runCurrent();vm.beginRotation(0f);vm.rotateVolume(60f);vm.foreground(false)
            assertFalse(vm.rotaryState.value.active)
            assertTrue(f.commands.none{it is YamahaCommand.SetVolume})
        }finally{Dispatchers.resetMain()}
    }
    @Test fun coordinateGestureThroughViewModelWritesOnceThenShowsReceiverClamp()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake().apply { max=43 }; val r=repo(f); val vm=ReceiverViewModel(r)
        try {
            runCurrent(); f.commands.clear()
            val gesture=RotaryTouchSession(220f,220f,vm::beginRotation,vm::rotateVolume,vm::finishRotation,vm::cancelRotation)
            assertTrue(gesture.down(130f,110f))
            for(a in 6..60 step 6) {
                val radians=Math.toRadians(a.toDouble())
                gesture.drag(110f+20*kotlin.math.cos(radians).toFloat(),110f+20*kotlin.math.sin(radians).toFloat())
            }
            assertEquals(45,vm.rotaryState.value.value)
            assertTrue(f.commands.none { it is YamahaCommand.SetVolume })
            gesture.up(110f,110f)
            advanceUntilIdle()
            assertEquals(listOf(YamahaCommand.SetVolume(Volume(45,0,""))),f.commands.filterIsInstance<YamahaCommand.SetVolume>())
            assertEquals(43,vm.rotaryState.value.value)
            assertEquals(YamahaCommand.Status,f.commands.last())
        } finally { vm.foreground(false);Dispatchers.resetMain() }
    }
    @Test fun unchangedAndCancelledCoordinateGesturesNeverWrite()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val vm=ReceiverViewModel(r)
        try {
            runCurrent();f.commands.clear()
            val g=RotaryTouchSession(220f,220f,vm::beginRotation,vm::rotateVolume,vm::finishRotation,vm::cancelRotation)
            g.down(130f,110f);g.up(130f,110f);advanceUntilIdle()
            g.down(130f,110f);g.drag(110f,130f);g.abort();advanceUntilIdle()
            assertEquals(40,vm.rotaryState.value.value)
            assertTrue(f.commands.none { it is YamahaCommand.SetVolume })
        } finally { vm.foreground(false);Dispatchers.resetMain() }
    }
    @Test fun unchangedFreshReceiverTargetSuppressesPut()=runTest {
        val f=Fake();val r=repo(f);f.commands.clear();r.setRotaryVolume(40)
        assertEquals(listOf(YamahaCommand.Status),f.commands)
    }
    @Test fun powerCommandsReadBackBothDirectionsAndRejectRapidClicks()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val vm=ReceiverViewModel(r)
        try {
            runCurrent();f.commands.clear()
            repeat(5) { vm.power(false) };advanceUntilIdle()
            assertEquals(listOf(YamahaCommand.Power(false)),f.commands.filterIsInstance<YamahaCommand.Power>())
            assertEquals(PowerState.STANDBY,vm.status.value.powerState)
            f.commands.clear()
            repeat(5) { vm.power(true) };advanceUntilIdle()
            assertEquals(listOf(YamahaCommand.Power(true)),f.commands.filterIsInstance<YamahaCommand.Power>())
            assertEquals(PowerState.ON,vm.status.value.powerState)
        } finally { vm.foreground(false);Dispatchers.resetMain() }
    }
    @Test fun successfulSourceRecordsRecentAndFailureDoesNot()=runTest{
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=Fake();val r=repo(f);val store=MemorySettingsStore()
        val settings=AppSettings(store);settings.lastSource("LINE1")
        val vm=ReceiverViewModel(r,settings=settings)
        try{
            runCurrent();assertTrue(f.commands.none{it is YamahaCommand.Input})
            vm.source("OPTICAL");advanceUntilIdle()
            assertEquals("OPTICAL",AppSettings(store).state.value.lastSource)
            f.failWrite=true;vm.source("LINE2");advanceUntilIdle()
            assertEquals("OPTICAL",settings.state.value.lastSource)
        }finally{vm.foreground(false);Dispatchers.resetMain()}
    }
}
