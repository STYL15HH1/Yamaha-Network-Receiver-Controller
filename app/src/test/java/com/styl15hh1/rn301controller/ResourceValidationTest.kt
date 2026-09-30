package com.styl15hh1.rn301controller

import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class ResourceValidationTest {
    private val resources = File("src/main/res")
    private fun strings(folder: String): Map<String,String> {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resources, "$folder/strings.xml"))
        val list = root.getElementsByTagName("string")
        return (0 until list.length).associate {
            val node = list.item(it)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }
    @Test fun eightResourceSetsHaveMatchingKeysAndFormatArguments() {
        val english = strings("values").filterKeys {
            !it.startsWith("language_") && it !in setOf("app_name","cd","spotify","airplay","github")
        }
        val format = Regex("%[0-9]+\\$[sd]")
        for (language in listOf("es","de","fr","it","pl","ko","ja")) {
            val translated = strings("values-$language")
            assertEquals(language, english.keys, translated.keys)
            for ((key,value) in english) {
                assertTrue("$language/$key", translated.getValue(key).isNotBlank())
                assertEquals("$language/$key", format.findAll(value).map { it.value }.toList().sorted(),
                    format.findAll(translated.getValue(key)).map { it.value }.toList().sorted())
            }
        }
        assertTrue(strings("values-ko").getValue("settings").contains("설정"))
        assertTrue(strings("values-ja").getValue("settings").contains("設定"))
    }
    @Test fun netRadioProductNameIsConsistentInEuropeanLocales() {
        val keys = listOf("loading_radio", "radio_service", "radio_timeout", "radio_inactive", "radio_open_hint")
        for (folder in listOf("values", "values-pl", "values-es", "values-de", "values-fr", "values-it")) {
            val values = strings(folder)
            assertEquals(folder, "Net Radio", values.getValue("net_radio"))
            keys.forEach { assertTrue("$folder/$it", values.getValue(it).contains("Net Radio")) }
        }
        assertEquals("インターネットラジオ", strings("values-ja").getValue("net_radio"))
        assertEquals("인터넷 라디오", strings("values-ko").getValue("net_radio"))
    }

    @Test fun allLauncherDensitiesHaveCorrectSize() {
        val densities = mapOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)
        for ((density,size) in densities) {
            for (name in listOf("ic_launcher","ic_launcher_foreground","ic_launcher_monochrome")) {
                val bitmap = ImageIO.read(File(resources, "mipmap-$density/$name.png"))
                val expected = if (name == "ic_launcher") size else size * 108 / 48
                assertEquals(expected, bitmap.width)
                assertEquals(expected, bitmap.height)
                if (name != "ic_launcher") assertTrue(bitmap.colorModel.hasAlpha())
            }
        }
        assertTrue(File(resources,"mipmap-anydpi/ic_launcher.xml").exists())
        assertTrue(File(resources,"mipmap-anydpi/ic_launcher.xml").readText().contains("monochrome"))
    }
    @Test fun languageConfigurationListsAllSupportedTags() {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resources,"xml/locales_config.xml"))
        val locales = root.getElementsByTagName("locale")
        val tags = (0 until locales.length).map { locales.item(it).attributes.getNamedItem("android:name").nodeValue }
        assertEquals(setOf("en","es","de","fr","it","pl","ko","ja"), tags.toSet())
    }
}
