package com.styl15hh1.rn301controller.data.settings

import com.styl15hh1.rn301controller.data.model.Source
import java.net.URLDecoder
import java.net.URLEncoder

object QuickSources {
    const val MAX = 4
    private val defaults = listOf("OPTICAL", "TUNER", "Spotify")
    /** Keep the original preference key/order, including an explicitly saved empty list. */
    fun ids(state: SettingsState, sources: List<Source>): List<String> =
        if (state.quickSourcesConfigured) state.quickSources.take(MAX)
        else available(defaults, sources).map { it.id }
    fun encode(ids: List<String>) = ids.distinct().take(MAX).joinToString(",") { URLEncoder.encode(it, "UTF-8") }
    fun decode(raw: String?): List<String> = raw.orEmpty().split(",").mapNotNull {
        try { URLDecoder.decode(it, "UTF-8").takeIf { id -> id.isNotBlank() && id.length <= 128 } }
        catch (_: IllegalArgumentException) { null }
    }.distinct().take(MAX)
    fun available(ids: List<String>, sources: List<Source>) =
        ids.mapNotNull { id -> sources.firstOrNull { it.id == id && it.selectable } }
}
class MemorySettingsStore : SettingsStore {
    private val values = mutableMapOf<String,String>()
    override fun read(key: String) = values[key]
    override fun write(key: String, value: String) { values[key] = value }
}
