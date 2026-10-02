package com.styl15hh1.rn301controller.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import com.styl15hh1.rn301controller.BuildConfig
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.data.model.CompatibilityText

@Composable
fun CompatibilityScreen(vm: ReceiverViewModel) {
    val report by vm.report.collectAsState()
    val busy by vm.busy.collectAsState()
    val address by vm.address.collectAsState()
    val context = LocalContext.current
    val resources = LocalResources.current
    var copied by remember(report) { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }
    Section(tr(R.string.report_title)) {
        Text(tr(R.string.report_hint))
        Text(tr(R.string.report_read_only))
        OutlinedTextField(value = address, onValueChange = vm::editAddress,
            label = { Text(tr(R.string.receiver)) }, singleLine = true, enabled = !busy,
            modifier = Modifier.fillMaxWidth())
        Button(onClick = vm::generateReport, enabled = !busy && address.isNotBlank()) { Text(tr(R.string.report_generate)) }
    }
    report?.let { result ->
        val text = CompatibilityText.format(result, BuildConfig.VERSION_NAME) { resources.getString(reportLabels.getValue(it)) }
        Section(tr(R.string.report_results)) {
            SelectionContainer { Text(text, style = MaterialTheme.typography.bodyMedium) }
            result.station?.let { Text(tr(R.string.report_station) + ": " + it) }
            Button(onClick = {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                    ClipData.newPlainText(resources.getString(R.string.report_title), text))
                copied = true
            }) { Text(tr(if (copied) R.string.report_copied else R.string.report_copy)) }
            OutlinedButton(onClick = {
                try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                }, resources.getString(R.string.report_share))) }
                catch (_: ActivityNotFoundException) { shareFailed = true }
            }) { Text(tr(R.string.report_share)) }
            if (shareFailed) Text(tr(R.string.open_link_failed))
        }
    }
}

internal val reportLabels = mapOf(
    "report_core" to R.string.report_core,
    "report_network" to R.string.report_network,
    "report_diagnostics" to R.string.report_diagnostics,
    "report_title" to R.string.report_title,
    "report_hint" to R.string.report_hint,
    "report_read_only" to R.string.report_read_only,
    "report_generate" to R.string.report_generate,
    "report_results" to R.string.report_results,
    "report_copy" to R.string.report_copy,
    "report_share" to R.string.report_share,
    "report_copied" to R.string.report_copied,
    "report_app_version" to R.string.report_app_version,
    "report_model" to R.string.report_model,
    "report_firmware" to R.string.report_firmware,
    "report_control_api" to R.string.report_control_api,
    "report_supported" to R.string.report_supported,
    "report_not_supported" to R.string.report_not_supported,
    "report_unknown" to R.string.report_unknown,
    "report_responded" to R.string.report_responded,
    "report_not_tested" to R.string.report_not_tested,
    "report_error" to R.string.report_error,
    "report_ready" to R.string.report_ready,
    "report_not_ready" to R.string.report_not_ready,
    "report_system" to R.string.report_system,
    "report_main_zone" to R.string.report_main_zone,
    "report_power" to R.string.report_power,
    "report_volume" to R.string.report_volume,
    "report_mute" to R.string.report_mute,
    "report_input" to R.string.report_input,
    "report_tuner" to R.string.report_tuner,
    "report_fm" to R.string.report_fm,
    "report_am" to R.string.report_am,
    "report_rds" to R.string.report_rds,
    "report_presets" to R.string.report_presets,
    "report_server" to R.string.report_server,
    "report_net_radio" to R.string.report_net_radio,
    "report_spotify" to R.string.report_spotify,
    "report_airplay" to R.string.report_airplay,
    "report_pandora" to R.string.report_pandora,
    "report_description" to R.string.report_description,
    "report_stored_presets" to R.string.report_stored_presets,
    "report_current_preset" to R.string.report_current_preset,
    "report_station" to R.string.report_station,
)
