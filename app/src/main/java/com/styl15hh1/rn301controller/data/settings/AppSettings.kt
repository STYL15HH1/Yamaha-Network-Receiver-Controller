package com.styl15hh1.rn301controller.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Appearance { SYSTEM, LIGHT, DARK }
object AppLanguages {
    val tags = listOf("en", "es", "de", "fr", "it", "pl", "ko", "ja")
    const val SYSTEM = "system"
    val choices get() = listOf(SYSTEM) + tags
    fun selection(tag: String?) = if (tag == SYSTEM) SYSTEM else supported(tag)
    fun supported(tag: String?) = tag?.substringBefore('-')?.takeIf { it in tags } ?: "en"
}
interface SettingsStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
}
data class SettingsState(val language: String = "en", val appearance: Appearance = Appearance.SYSTEM,
    val quickSources: List<String> = emptyList(), val lastSource: String? = null,
    val quickSourcesConfigured: Boolean = false,
    val volumeExpanded: Boolean = false, val sourcesExpanded: Boolean = false)
class AppSettings(private val store: SettingsStore) {
    private val mutable = MutableStateFlow(SettingsState(
        AppLanguages.selection(store.read("language")),
        Appearance.entries.firstOrNull { it.name == store.read("appearance") } ?: Appearance.SYSTEM,
        QuickSources.decode(store.read("quick_sources")), store.read("last_source")?.takeIf { it.isNotBlank() }, store.read("quick_sources") != null,
        volumeExpanded = store.read("volume_expanded") == "true",
        sourcesExpanded = store.read("sources_expanded") == "true"
    ))
    init {
        // Persist the bounded migration once; preserve the key, order and explicit empty choice.
        store.read("quick_sources")?.let { raw ->
            val migrated = QuickSources.encode(mutable.value.quickSources)
            if (raw != migrated) store.write("quick_sources", migrated)
        }
    }
    val state = mutable.asStateFlow()
    fun quickSources(ids: List<String>) {
        val encoded = QuickSources.encode(ids.filter { it.isNotBlank() && it.length <= 128 })
        store.write("quick_sources", encoded)
        mutable.value = mutable.value.copy(quickSources = QuickSources.decode(encoded), quickSourcesConfigured = true)
    }
    fun volumeExpanded(value: Boolean) {
        store.write("volume_expanded", value.toString())
        mutable.value = mutable.value.copy(volumeExpanded = value)
    }
    fun sourcesExpanded(value: Boolean) {
        store.write("sources_expanded", value.toString())
        mutable.value = mutable.value.copy(sourcesExpanded = value)
    }
    fun lastSource(id: String) {
        store.write("last_source", id)
        mutable.value = mutable.value.copy(lastSource = id)
    }
    fun language(tag: String) {
        val selected = AppLanguages.selection(tag)
        store.write("language", selected)
        mutable.value = mutable.value.copy(language = selected)
    }
    fun appearance(value: Appearance) {
        store.write("appearance", value.name)
        mutable.value = mutable.value.copy(appearance = value)
    }
}
data class AboutInfo(val version: String, val author: String = "STYL15HH1", val github: String = "https://github.com/STYL15HH1")
