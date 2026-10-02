package com.styl15hh1.rn301controller.data.protocol

import com.styl15hh1.rn301controller.data.model.*
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class Capabilities(val model: String?, val paths: Set<String>, val volumeRange: VolumeRange?)
data class SystemConfig(val model: String?, val volumeRange: VolumeRange?, val firmware: String? = null, val features: Map<String, Boolean?> = emptyMap())

class YamahaXmlParser {
    private fun document(xml: String): Element {
        if (xml.length > 512 * 1024 || xml.contains("<!DOCTYPE", true) || xml.contains("<!ENTITY", true))
            invalid("Oversized XML or forbidden declaration")
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            factory.isExpandEntityReferences = false
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, _ -> InputSource(StringReader("")) }
            builder.setErrorHandler(object : DefaultHandler() {
                override fun error(e: SAXParseException) { throw e }
                override fun fatalError(e: SAXParseException) { throw e }
            })
            builder.parse(InputSource(StringReader(xml))).documentElement
        } catch (e: Exception) {
            throw YamahaException(ReceiverError(ErrorKind.INVALID_RESPONSE, "XML parse: ${e.javaClass.simpleName}"), e)
        }
    }

    fun response(xml: String, expected: String, allowMissingRc: Boolean = false): Element {
        val root = document(xml)
        if (root.tagName != "YAMAHA_AV" || root.getAttribute("rsp") != expected) invalid("Unexpected response envelope")
        val rc = root.getAttribute("RC")
        if (rc.isEmpty() && allowMissingRc) return root
        if (rc.toIntOrNull() == null) invalid("Missing or invalid RC")
        if (rc != "0") throw YamahaException(ReceiverError(ErrorKind.UNSUPPORTED_COMMAND, "Yamaha RC=$rc"))
        return root
    }

    fun status(xml: String): ReceiverStatus {
        val basic = response(xml, "GET").path("Main_Zone", "Basic_Status") ?: invalid("Missing Basic_Status")
        val power = when (basic.path("Power_Control", "Power")?.textContent?.trim()) {
            "On" -> PowerState.ON
            "Standby" -> PowerState.STANDBY
            else -> invalid("Missing or unknown power")
        }
        val volume = basic.path("Volume", "Lvl")?.let { lvl ->
            val value = lvl.child("Val")?.textContent?.trim()?.toIntOrNull() ?: invalid("Invalid volume Val")
            val exp = lvl.child("Exp")?.textContent?.trim()?.toIntOrNull() ?: invalid("Invalid volume Exp")
            val unit = lvl.child("Unit")?.textContent?.trim() ?: invalid("Missing volume Unit")
            if (exp !in 0..3 || value !in -100000..100000 || unit !in setOf("", "dB")) invalid("Unsupported volume encoding")
            Volume(value, exp, unit)
        }
        val mute = basic.path("Volume", "Mute")?.let {
            when (it.textContent.trim()) { "On" -> true; "Off" -> false; else -> invalid("Invalid mute") }
        }
        val source = basic.path("Input", "Input_Sel")?.textContent?.trim()?.takeIf { it.isNotEmpty() }?.let { Source(it) }
        if (power == PowerState.ON && (volume == null || mute == null || source == null)) invalid("Incomplete powered-on status")
        return ReceiverStatus(powerState = power, volume = volume, muted = mute, currentSource = source)
    }

    fun spotify(xml: String) = playerInfo(xml, "Spotify")
    fun server(xml: String) = playerInfo(xml, "SERVER")
    fun netRadio(xml: String) = playerInfo(xml, "NET_RADIO").copy(source = "NET RADIO")
    private fun playerInfo(xml: String, source: String): NowPlaying {
        val info = response(xml, "GET").path(source, "Play_Info") ?: invalid("Missing $source Play_Info")
        fun text(vararg path: String) = info.path(*path)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
        return NowPlaying(
            source = source, title = text("Meta_Info", "Track") ?: text("Meta_Info", "Song") ?: text("Meta_Info", "Title"),
            artist = text("Meta_Info", "Artist"), album = text("Meta_Info", "Album"), station = text("Meta_Info", "Station"),
            playbackState = text("Playback_Info"), availability = text("Feature_Availability"),
            inputLogo = InputLogo(text("Input_Logo", "URL_S"), text("Input_Logo", "URL_M"), text("Input_Logo", "URL_L")),
            shuffle = when (text("Play_Mode", "Shuffle")) { "On" -> true; "Off" -> false; else -> null },
            repeat = when (text("Play_Mode", "Repeat")) {
                null -> null; "Off" -> RepeatMode.OFF; "One" -> RepeatMode.ONE; "All" -> RepeatMode.ALL
                else -> RepeatMode.UNKNOWN
            }
        )
    }

    fun serverList(xml: String) = mediaList(xml, "SERVER")
    fun netRadioList(xml: String) = mediaList(xml, "NET_RADIO")
    private fun mediaList(xml: String, scope: String): MediaList {
        val source = if (scope == "NET_RADIO") "NET RADIO" else scope
        val info = response(xml, "GET").path(scope, "List_Info") ?: invalid("Missing $scope List_Info")
        fun text(vararg path: String) = info.path(*path)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
        val menu = text("Menu_Status") ?: invalid("Missing Menu_Status")
        fun number(vararg path: String): Int? {
            val value = text(*path) ?: return null
            return value.toIntOrNull()?.takeIf { it in 0..1000000 } ?: invalid("Invalid list position")
        }
        val layer = number("Menu_Layer")
        val current = number("Cursor_Position", "Current_Line")
        val max = number("Cursor_Position", "Max_Line")
        if (menu == "Ready" && (layer == null || layer < 1 || current == null || max == null ||
            (max > 0 && current !in 1..max))) invalid("Incomplete list position")
        val entries = info.child("Current_List")?.children().orEmpty()
        val items = entries.mapNotNull { line ->
            val index = Regex("Line_([1-8])").matchEntire(line.tagName)?.groupValues?.get(1)?.toInt()
                ?: invalid("Invalid list line")
            val title = line.child("Txt")?.textContent?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            MediaItem(index, title, line.child("Attribute")?.textContent?.trim(), source)
        }.sortedBy { it.line }
        if (items.map { it.line }.distinct().size != items.size) invalid("Duplicate list line")
        if (menu == "Ready" && max != null && (max == 0 && items.isNotEmpty())) invalid("Invalid empty list")
        return MediaList(source, menu, layer, text("Menu_Name"), current, max, items)
    }

    fun tunerConfig(xml: String, band: TunerBand = TunerBand.FM): TuningRange? {
        val config = response(xml, "GET").path("Tuner", "Config") ?: invalid("Missing Tuner Config")
        val fm = config.path("Range_and_Step", band.name) ?: return null
        fun number(name: String): Int {
            val item = fm.child(name) ?: invalid("Incomplete FM range")
            val value = item.child("Val")?.textContent?.trim()?.toLongOrNull() ?: invalid("Invalid FM range value")
            val exp = item.child("Exp")?.textContent?.trim()?.toIntOrNull() ?: invalid("Invalid FM range exponent")
            if (exp !in 0..6 || item.child("Unit")?.textContent?.trim() != band.unit) invalid("Invalid FM range unit")
            return try { java.math.BigDecimal.valueOf(value, exp).movePointRight(band.exponent).intValueExact() }
                catch (_: ArithmeticException) { invalid("Unsupported FM precision") }
        }
        return try { TuningRange(number("Min"), number("Max"), number("Step"), band) }
            catch (_: IllegalArgumentException) { invalid("Invalid FM range") }
    }

    fun tuner(xml: String): TunerStatus {
        val info = response(xml, "GET").path("Tuner", "Play_Info") ?: invalid("Missing tuner Play_Info")
        fun text(vararg path: String) = info.path(*path)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
        fun signal(name: String): Boolean? = when (text("Signal_Info", name)) {
            "Assert" -> true; "Negate" -> false; else -> null
        }
        val tuningText = text("Tuning", "Freq", "Current", "Val")
        val frequency = info.path("Tuning", "Freq", "Current")?.let {
            if (tuningText in setOf("Auto Up", "Auto Down", "TP Up", "TP Down")) return@let null
            val value = it.child("Val")?.textContent?.trim()?.toIntOrNull() ?: invalid("Invalid tuner frequency")
            val exp = it.child("Exp")?.textContent?.trim()?.toIntOrNull() ?: invalid("Invalid tuner exponent")
            val unit = it.child("Unit")?.textContent?.trim() ?: invalid("Missing frequency unit")
            if (value <= 0 || exp !in 0..6 || unit !in setOf("MHz", "kHz", "Hz")) invalid("Invalid frequency encoding")
            ReceiverFrequency(value, exp, unit)
        }
        val presetText = text("Preset", "Preset_Sel")
        val preset = presetText?.toIntOrNull()?.takeIf { it in 1..40 }
        val band = text("Tuning", "Band")
        fun rds(name: String) = if (band == "FM") text("Meta_Info", name)?.takeUnless { it in setOf("-", "--") } else null
        val station = rds("Program_Service")
        val program = listOfNotNull(rds("Radio_Text_A"), rds("Radio_Text_B"))
            .filterNot { it.equals(station, ignoreCase = true) }.distinctBy { it.lowercase(java.util.Locale.ROOT) }
            .joinToString(" · ").ifBlank { null }
        return TunerStatus(
            band = text("Tuning", "Band"), frequency = frequency, preset = preset,
            tuningState = tuningText?.takeIf { it in setOf("Auto Up", "Auto Down", "TP Up", "TP Down") },
            availability = text("Feature_Availability"), tuned = signal("Tuned"), stereo = signal("Stereo"),
            programType = rds("Program_Type"),
            fmMode = if (band == "FM") FmMode.entries.firstOrNull { it.wire == text("FM_Mode") } else null,
            clockTime = rds("Clock_Time"),
            nowPlaying = NowPlaying("TUNER", title = program, station = station,
                playbackState = text("Feature_Availability"))
        )
    }

    fun tunerPresets(xml: String): List<TunerPreset> {
        val node = response(xml, "GET").path("Tuner", "Play_Control", "Preset", "Preset_Sel_Item")
            ?: invalid("Missing tuner preset list")
        return node.children().mapNotNull { item ->
            val id = item.child("Param")?.textContent?.trim()?.toIntOrNull() ?: return@mapNotNull null
            if (id !in 1..40 || item.child("RW")?.textContent?.contains("W") != true) return@mapNotNull null
            TunerPreset(id, item.child("Title")?.textContent?.trim()?.takeIf { it.isNotEmpty() } ?: "Preset $id")
        }.distinctBy { it.number }.sortedBy { it.number }
    }

    fun upnpIdentity(xml: String): UpnpIdentity? {
        val root = document(xml)
        if (root.tagName.substringAfter(':') != "root") invalid("Not a UPnP description")
        val device = root.children().firstOrNull { it.tagName.substringAfter(':') == "device" } ?: return null
        fun value(name: String) = device.children().firstOrNull { it.tagName.substringAfter(':') == name }?.textContent?.trim().orEmpty()
        return UpnpIdentity(value("manufacturer"), value("modelName"), value("friendlyName"),
            value("UDN"), value("deviceType"))
    }
    fun config(xml: String): SystemConfig {
        val node = response(xml, "GET", allowMissingRc = true).path("System", "Config") ?: invalid("Missing System Config")
        val volume = node.child("Volume")
        val range = try {
            if (volume == null) null else VolumeRange(
                volume.child("Min")!!.textContent.trim().toInt(),
                volume.child("Max")!!.textContent.trim().toInt(),
                volume.child("Step")!!.textContent.trim().toInt(), 0, ""
            )
        } catch (_: Exception) { null }
        return SystemConfig(node.child("Model_Name")?.textContent?.trim()?.takeIf { it.isNotEmpty() }, range,
            node.child("Version")?.textContent?.trim()?.takeIf { it.isNotEmpty() },
            node.child("Feature_Existence")?.children().orEmpty().associate {
                it.tagName to when (it.textContent.trim()) { "1" -> true; "0" -> false; else -> null }
            })
    }

    fun featureAvailability(xml: String, source: String, node: String): String? =
        (response(xml, "GET").path(source, node) ?: invalid("Missing feature node"))
            .child("Feature_Availability")?.textContent?.trim()

    fun rdsCapability(xml: String): Boolean? = when (
        (response(xml, "GET").path("Tuner", "Config") ?: invalid("Missing Tuner Config"))
            .child("RDS")?.textContent?.trim()) {
        "Exist" -> true; "Not Exist" -> false; else -> null
    }

    fun inputs(xml: String): List<Source> {
        val node = response(xml, "GET").path("Main_Zone", "Input", "Input_Sel_Item") ?: invalid("Missing input list")
        return node.children().mapNotNull { item ->
            val id = item.child("Param")?.textContent?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val title = item.child("Title")?.textContent?.trim()?.takeIf { it.isNotEmpty() } ?: Source.labels[id] ?: id
            Source(id, title, item.child("RW")?.textContent?.contains("W") != false)
        }.distinctBy { it.id }.also { if (it.isEmpty()) invalid("Empty input list") }
    }

    fun description(xml: String): Capabilities {
        val root = document(xml)
        if (root.tagName != "Unit_Description") invalid("Not a Yamaha Unit_Description")
        val paths = root.descendants("Define").map { it.textContent.trim() }.toSet()
        var range: VolumeRange? = null
        // IDs are scoped to each unit menu; never match a tuner/bass/other-zone range.
        for (menu in root.children().filter { it.tagName == "Menu" }) {
            val id = menu.child("Cmd_List")?.children()?.firstOrNull {
                it.tagName == "Define" && it.textContent.trim() == "Main_Zone,Volume,Lvl"
            }?.getAttribute("ID") ?: continue
            for (put in menu.descendants("Put_2")) {
                val cmd = put.child("Cmd") ?: continue
                if (cmd.getAttribute("ID") != id || cmd.textContent.trim() != "Val=Param_1:Exp=Param_2:Unit=Param_3") continue
                range = try {
                    val limits = put.path("Param_1", "Range")!!.textContent.trim().split(",").map { it.trim().toInt() }
                    val exp = put.path("Param_2", "Direct")!!.textContent.trim().toInt()
                    val unit = put.path("Param_3", "Direct")!!.textContent.trim()
                    if (limits.size != 3 || exp !in 0..3 || unit !in setOf("", "dB")) null
                    else VolumeRange(limits[0], limits[1], limits[2], exp, unit)
                } catch (_: Exception) { null }
            }
        }
        return Capabilities(root.getAttribute("Unit_Name").takeIf { it.isNotBlank() }, paths, range)
    }
}
private fun Element.children(): List<Element> = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
private fun Element.child(name: String): Element? = children().firstOrNull { it.tagName == name }
private fun Element.path(vararg names: String): Element? = names.fold(this as Element?) { node, name -> node?.child(name) }
private fun Element.descendants(name: String): List<Element> = getElementsByTagName(name).let { nodes ->
    (0 until nodes.length).map { nodes.item(it) as Element }
}

data class UpnpIdentity(val manufacturer: String, val model: String, val name: String, val udn: String, val deviceType: String) {
    val supported: Boolean get() = manufacturer.equals("Yamaha Corporation", true) &&
        model.equals("R-N301", true) && deviceType == "urn:schemas-upnp-org:device:MediaRenderer:1"
}