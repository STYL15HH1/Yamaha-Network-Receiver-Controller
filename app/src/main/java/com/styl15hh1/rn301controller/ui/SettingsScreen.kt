package com.styl15hh1.rn301controller.ui

import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.styl15hh1.rn301controller.BuildConfig
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.settings.*

@Composable
fun SettingsScreen(vm: ReceiverViewModel, settings: AppSettings, selectLanguage: (String) -> Unit) {
    val preferences by settings.state.collectAsState()
    val receiver by vm.status.collectAsState()
    var languages by remember { mutableStateOf(false) }
    var themes by remember { mutableStateOf(false) }
    var quick by remember { mutableStateOf(false) }

    Section(tr(R.string.general)) {
        SettingsRow(tr(R.string.language), languageName(preferences.language)) { languages = true }
        SettingsRow(tr(R.string.quick_sources), tr(R.string.quick_sources_hint, QuickSources.MAX)) { quick = true }
        SettingsRow(tr(R.string.appearance), appearanceName(preferences.appearance)) { themes = true }
    }
    Section(tr(R.string.receiver)) {
        Text(tr(R.string.connected_receiver))
        Text(tr(if (receiver.connectionState == com.styl15hh1.rn301controller.data.model.ConnectionState.CONNECTED)
            R.string.connected else R.string.disconnected))
        Text(receiver.modelName ?: tr(R.string.disconnected))
        if (receiver.address.isNotBlank()) Text(receiver.address)
        val busy by vm.busy.collectAsState()
        OutlinedButton(onClick = { vm.connect() }, enabled = !busy && receiver.address.isNotBlank()) { Text(tr(R.string.test_connection)) }
    }
    ConnectionControls(vm)
    Section(tr(R.string.application)) {
        SettingsRow(tr(R.string.about)) { vm.about() }
        Text(tr(R.string.version, BuildConfig.VERSION_NAME))
    }
    if (quick) QuickSourceChoices(settings, receiver.sources) { quick = false }
    if (languages) AlertDialog(onDismissRequest = { languages = false },
        title = { Text(tr(R.string.application_language)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppLanguages.choices.forEach { tag ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(
                        selected = tag == preferences.language, role = Role.RadioButton,
                        onClick = { languages = false; if (tag != preferences.language) selectLanguage(tag) }),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tag == preferences.language, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(languageName(tag))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { languages = false }) { Text(tr(R.string.dismiss)) } })
    if (themes) AlertDialog(onDismissRequest = { themes = false },
        title = { Text(tr(R.string.appearance)) },
        text = {
            Column {
                Appearance.entries.forEach { theme ->
                    TextButton(onClick = { settings.appearance(theme); themes = false }) { Text(appearanceName(theme)) }
                }
            }
        }, confirmButton = { TextButton(onClick = { themes = false }) { Text(tr(R.string.dismiss)) } })
}
@Composable private fun SettingsRow(title: String, subtitle: String? = null, action: () -> Unit) {
    ListItem(modifier = Modifier.fillMaxWidth().clickable(onClick = action),
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null) })
}
@Composable private fun languageName(tag: String) = tr(when(tag) {
    AppLanguages.SYSTEM -> R.string.system_default
    "es" -> R.string.language_es; "de" -> R.string.language_de; "fr" -> R.string.language_fr
    "it" -> R.string.language_it; "pl" -> R.string.language_pl; "ko" -> R.string.language_ko
    "ja" -> R.string.language_ja; else -> R.string.language_en
})
@Composable private fun appearanceName(value: Appearance) = tr(when(value) {
    Appearance.SYSTEM -> R.string.system_theme; Appearance.LIGHT -> R.string.light_theme; Appearance.DARK -> R.string.dark_theme
})
@Composable fun AboutSection() {
    val info = AboutInfo(BuildConfig.VERSION_NAME)
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    Section(tr(R.string.about)) {
        Text(tr(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Text(tr(R.string.version, info.version))
        Text(tr(R.string.author) + ": " + info.author)
        Text(tr(R.string.unofficial))
        TextButton(onClick = {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, info.github.toUri())) }
            catch (_: ActivityNotFoundException) { failed = true }
        }) { Text(tr(R.string.github) + " · " + info.github) }
        if (failed) Text(tr(R.string.open_link_failed))
    }
}
