package com.styl15hh1.rn301controller.data.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit

class PreferencesSettingsStore(context: Context) : SettingsStore {
    private val preferences = context.getSharedPreferences("application_settings", Context.MODE_PRIVATE)
    override fun read(key: String): String? = preferences.getString(key, null)
    override fun write(key: String, value: String) { preferences.edit { putString(key, value) } }
}
object AndroidLocales {
    fun context(base: Context): Context {
        val saved = AppLanguages.selection(PreferencesSettingsStore(base).read("language"))
        val platform = if (Build.VERSION.SDK_INT >= 33)
            base.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags() else ""
        if (platform.isBlank() && saved == AppLanguages.SYSTEM) return base
        val tag = if (platform.isNotBlank()) AppLanguages.supported(platform) else saved
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tag))
        return base.createConfigurationContext(config)
    }
    fun select(activity: Activity, settings: AppSettings, tag: String) {
        val selected = AppLanguages.selection(tag)
        settings.language(selected)
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = activity.getSystemService(LocaleManager::class.java)
            val locales = if (selected == AppLanguages.SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(selected)
            if (manager.applicationLocales == locales) activity.recreate() else manager.applicationLocales = locales
        } else activity.recreate()
    }
    fun syncPlatform(context: Context, settings: AppSettings) {
        if (Build.VERSION.SDK_INT >= 33) {
            val tags = context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
            if (tags.isNotBlank()) settings.language(AppLanguages.supported(tags))
        }
    }
}
