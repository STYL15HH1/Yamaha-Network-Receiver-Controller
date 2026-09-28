package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.network.ReceiverAddress
import java.net.InetAddress
import org.junit.Assert.*
import org.junit.Test

/** Synthetic fixtures follow the reference paths; they are NOT receiver captures. */
class ProtocolTest {
    private val parser = YamahaXmlParser()
    @Test fun powerOn() { assertEquals(PowerState.ON, parser.status(statusXml()).powerState) }
    @Test fun standby() { assertEquals(PowerState.STANDBY, parser.status(statusXml(power = "Standby")).powerState) }
    @Test fun unknownPowerRejected() { invalid { parser.status(statusXml(power = "Sleeping")) } }
    @Test fun decibels() {
        val volume = parser.status(statusXml(value = -325, exp = 1, unit = "dB")).volume!!
        assertEquals(-32.5, volume.db!!, 0.001)
        assertEquals("-32.5 dB", volume.display)
        assertEquals(-325, volume.value)
    }
    @Test fun nativeVolumeIsNotFalselyLabelledDb() {
        val volume = parser.status(statusXml(value = 25)).volume!!
        assertNull(volume.db)
        assertEquals("25 (native)", volume.display)
    }
    @Test fun muteStates() {
        assertEquals(true, parser.status(statusXml(mute = "On")).muted)
        assertEquals(false, parser.status(statusXml()).muted)
    }
    @Test fun source() { assertEquals("NET RADIO", parser.status(statusXml()).currentSource!!.id) }
    @Test fun unknownSource() {
        assertEquals("New input", parser.status(statusXml(source = "New input")).currentSource!!.title)
    }
    @Test fun malformedXml() { invalid { parser.status("<YAMAHA_AV>") } }
    @Test fun missingFields() { invalid { parser.status("<YAMAHA_AV rsp=\"GET\" RC=\"0\"><Main_Zone><Basic_Status/></Main_Zone></YAMAHA_AV>") } }
    @Test fun invalidNumber() { invalid { parser.status(statusXml().replace("<Val>25", "<Val>NaN")) } }
    @Test fun missingReturnCode() { invalid { parser.status(statusXml().replace(" RC=\"0\"", "")) } }
    @Test fun wrongEnvelope() { invalid { parser.status("<html/>") } }
    @Test fun wrongResponseType() { invalid { parser.status(statusXml().replace("rsp=\"GET\"", "rsp=\"PUT\"")) } }
    @Test fun rejectsDtd() { invalid { parser.status("<!DOCTYPE YAMAHA_AV [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>" + statusXml()) } }
    @Test fun nonzeroRc() {
        val ex = assertThrows(YamahaException::class.java) { parser.response("<YAMAHA_AV rsp=\"PUT\" RC=\"4\"/>", "PUT") }
        assertEquals(ErrorKind.UNSUPPORTED_COMMAND, ex.error.kind)
    }
    @Test fun acknowledgement() { parser.response("<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>", "PUT") }
    @Test fun systemConfig() {
        val config = parser.config(configXml)
        assertEquals("R-N301", config.model)
        assertEquals(1, config.volumeRange!!.step)
    }
    @Test fun configWithoutRc() { assertEquals("R-N301", parser.config(configXml.replace(" RC=\"0\"", "")).model) }
    @Test fun capabilityRange() {
        val caps = parser.description(descriptionXml)
        assertEquals(VolumeRange(-805, 165, 5, 1, "dB"), caps.volumeRange)
    }
    @Test fun advertisedInputs() {
        val sources = parser.inputs("""<YAMAHA_AV rsp="GET" RC="0"><Main_Zone><Input><Input_Sel_Item>
            <Item_1><Param>CD</Param><Title>Disc</Title><RW>RW</RW></Item_1>
            <Item_2><Param>Future</Param><Title/><RW>R</RW></Item_2>
            </Input_Sel_Item></Input></Main_Zone></YAMAHA_AV>""")
        assertEquals("Disc", sources[0].title)
        assertEquals("Future", sources[1].title)
        assertFalse(sources[1].selectable)
    }
    @Test fun exactPowerAndMutePaths() {
        assertTrue(YamahaXmlBuilder.build(YamahaCommand.Power(false)).contains("<System><Power_Control><Power>Standby</Power></Power_Control></System>"))
        assertTrue(YamahaXmlBuilder.build(YamahaCommand.Power(true)).contains("<Power>On</Power>"))
        assertTrue(YamahaXmlBuilder.build(YamahaCommand.Mute(true)).contains("<Main_Zone><Volume><Mute>On</Mute></Volume></Main_Zone>"))
    }
    @Test fun exactNativeVolume() {
        assertTrue(YamahaXmlBuilder.build(YamahaCommand.SetVolume(Volume(25, 0, "")))
            .contains("<Main_Zone><Volume><Lvl><Val>25</Val><Exp>0</Exp><Unit></Unit></Lvl></Volume></Main_Zone>"))
    }
    @Test fun sourceEscaped() {
        assertTrue(YamahaXmlBuilder.build(YamahaCommand.Input("A&B<CD>")).contains("A&amp;B&lt;CD&gt;"))
    }
    @Test fun addressValidation() {
        listOf("192.168.1.100", "receiver", "yamaha.local").forEach { ReceiverAddress.parse(it) }
        listOf("", "http://192.168.1.2", "256.1.1.1", "1.2.3", "192.168.1.1:80", "user@receiver", "../receiver", "01.2.3.4").forEach {
            assertThrows(YamahaException::class.java) { ReceiverAddress.parse(it) }
        }
    }
    @Test fun privateDestinationsOnly() {
        listOf("192.168.1.1", "10.0.0.1", "172.16.1.1", "169.254.1.2").forEach { assertTrue(ReceiverAddress.isLocal(InetAddress.getByName(it))) }
        listOf("127.0.0.1", "8.8.8.8", "224.0.0.1", "172.32.1.1", "::1").forEach { assertFalse(ReceiverAddress.isLocal(InetAddress.getByName(it))) }
    }
    private fun invalid(block: () -> Unit) {
        assertEquals(ErrorKind.INVALID_RESPONSE, assertThrows(YamahaException::class.java, block).error.kind)
    }
}

internal fun statusXml(power: String = "On", value: Int = 25, exp: Int = 0, unit: String = "", mute: String = "Off", source: String = "NET RADIO") =
    """<YAMAHA_AV rsp="GET" RC="0"><Main_Zone><Basic_Status>
    <Power_Control><Power>$power</Power></Power_Control>
    <Volume><Lvl><Val>$value</Val><Exp>$exp</Exp><Unit>$unit</Unit></Lvl><Mute>$mute</Mute></Volume>
    <Input><Input_Sel>$source</Input_Sel></Input></Basic_Status></Main_Zone></YAMAHA_AV>"""
internal val configXml = """<YAMAHA_AV rsp="GET" RC="0"><System><Config><Model_Name>R-N301</Model_Name>
    <Volume><Min>0</Min><Max>100</Max><Step>1</Step></Volume></Config></System></YAMAHA_AV>"""
internal val descriptionXml = """<Unit_Description Unit_Name="R-N301"><Menu>
    <Menu Func="Vol_Lvl"><Put_2><Cmd ID="P2">Val=Param_1:Exp=Param_2:Unit=Param_3</Cmd>
    <Param_1><Range>-805,165,5</Range></Param_1><Param_2><Direct>1</Direct></Param_2>
    <Param_3><Direct>dB</Direct></Param_3></Put_2></Menu>
    <Cmd_List><Define ID="P2">Main_Zone,Volume,Lvl</Define></Cmd_List></Menu></Unit_Description>"""
