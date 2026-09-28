package com.styl15hh1.rn301controller.data.model

import java.util.Locale

/** Offline ISO names plus conservative catalogue aliases. Never inferred from station names. */
object CountryFlags {
    private fun key(value: String) = value.trim().lowercase(Locale.ROOT)
    private val countryMenus = setOf("countries", "country", "by country", "browse by country",
        "países", "länder", "pays", "paesi", "kraje", "국가", "国", "国別")
    private val names: Map<String, String> by lazy {
        buildMap {
            val languages = listOf("en", "es", "de", "fr", "it", "pl", "ko", "ja").map { Locale.forLanguageTag(it) }
            for (code in Locale.getISOCountries()) {
                val country = Locale.Builder().setRegion(code).build()
                languages.forEach { put(key(country.getDisplayCountry(it)), code) }
            }
            put("south korea", "KR"); put("north korea", "KP")
            put("czech republic", "CZ"); put("ivory coast", "CI")
            put("burma", "MM"); put("myanmar (burma)", "MM")
            put("usa", "US"); put("united states of america", "US"); put("uk", "GB")
        }
    }
    fun code(name: String): String? = names[key(name)]
    fun flag(name: String): String? = code(name)?.let { code ->
        code.map { Character.toChars(0x1F1E6 + it.code - 'A'.code).concatToString() }.joinToString("")
    }
    fun isCountryMenu(list: MediaList?) = list?.source == "NET RADIO" && key(list.title.orEmpty()) in countryMenus
    fun forEntry(list: MediaList?, item: MediaItem): String? =
        if (isCountryMenu(list) && item.browsable)
            flag(item.title) else null
}
