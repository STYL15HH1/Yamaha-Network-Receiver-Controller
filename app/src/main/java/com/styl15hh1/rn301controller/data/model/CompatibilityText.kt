package com.styl15hh1.rn301controller.data.model

/** Labels are supplied by Android resources (or a test); values are an export allowlist. */
object CompatibilityText {
    fun format(report: CompatibilityReport, version: String, label: (String) -> String): String = buildString {
        fun line(key: String, value: String) { append(label(key)).append(": ").append(value).append('\n') }
        append("Yamaha Receiver Controller — ").append(label("report_title")).append("\n\n")
        line("report_app_version", version.takeIf { it.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")) } ?: "?")
        line("report_model", ReportPrivacy.model(report.model) ?: label("report_unknown"))
        line("report_firmware", ReportPrivacy.firmware(report.firmware) ?: label("report_unknown"))
        line("report_control_api", label(if (report.controlApiResponded) "report_responded" else "report_not_tested"))
        append('\n').append(label("report_read_only")).append("\n\n")
        val groups = listOf(
            "report_core" to setOf(ReportItem.MAIN_ZONE, ReportItem.POWER, ReportItem.VOLUME, ReportItem.MUTE, ReportItem.INPUT),
            "report_tuner" to setOf(ReportItem.TUNER, ReportItem.FM, ReportItem.AM, ReportItem.RDS, ReportItem.PRESETS),
            "report_network" to setOf(ReportItem.SERVER, ReportItem.NET_RADIO, ReportItem.SPOTIFY, ReportItem.AIRPLAY, ReportItem.PANDORA),
            "report_diagnostics" to setOf(ReportItem.SYSTEM, ReportItem.DESCRIPTION)
        )
        for ((heading, items) in groups) {
            append(label(heading)).append('\n')
            for (row in report.evidence.filter { it.item in items }) {
                append(label("report_" + row.item.name.lowercase(java.util.Locale.ROOT)))
                row.node?.takeIf { it in setOf("Config", "Play_Info") }?.let { append(" / ").append(it) }
                append(": ").append(label("report_" + row.capability.name.lowercase(java.util.Locale.ROOT)))
                append("; ").append(label("report_" + row.probe.name.lowercase(java.util.Locale.ROOT)))
                if (row.availability != RuntimeAvailability.UNKNOWN)
                    append("; ").append(label("report_" + row.availability.name.lowercase(java.util.Locale.ROOT)))
                row.error?.let { append(" [").append(it.name).append(']') }
                append('\n')
            }
            if (ReportItem.TUNER in items) {
                report.storedPresets?.let { line("report_stored_presets", it.toString()) }
                report.currentPreset?.let { line("report_current_preset", it.toString()) }
            }
            append('\n')
        }
    }
}
