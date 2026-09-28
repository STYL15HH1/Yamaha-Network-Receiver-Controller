package com.styl15hh1.rn301controller.data.model

import java.math.BigDecimal

/** Normalized per-band wire format from receiver ajxSetTunerFreq and regional Config. */
typealias FmRange = TuningRange

enum class TunerBand(val exponent: Int, val unit: String) { FM(2, "MHz"), AM(0, "kHz") }
enum class FmMode(val wire: String) { AUTO("Auto"), MONO("Mono") }

data class TuningRange(val min: Int, val max: Int, val step: Int, val band: TunerBand = TunerBand.FM) {
    init { require(min > 0 && max > min && max <= (if (band == TunerBand.FM) 20000 else 3000) && step in 1..(max - min)) }
    fun accepts(value: Int) = value in min..max && (value - min) % step == 0
    fun parse(text: String): Int? = try {
        BigDecimal(text.trim().replace(',', '.')).movePointRight(band.exponent).intValueExact().takeIf(::accepts)
    } catch (_: NumberFormatException) { null } catch (_: ArithmeticException) { null }
    val stepDisplay get() = ReceiverFrequency(step, band.exponent, band.unit).display
    val minDisplay get() = ReceiverFrequency(min, band.exponent, band.unit).display
    val maxDisplay get() = ReceiverFrequency(max, band.exponent, band.unit).display
    fun stepFrom(current: ReceiverFrequency?, direction: Int): Int? {
        if (current == null || current.unit != band.unit || direction !in listOf(-1, 1)) return null
        val value = try { BigDecimal.valueOf(current.value.toLong(), current.exponent).movePointRight(band.exponent).intValueExact() }
            catch (_: ArithmeticException) { return null }
        return (value + direction * step).takeIf { accepts(value) && accepts(it) }
    }
}
data class PresetPresentation(val number: Int, val station: String?, val frequency: String?, val selected: Boolean)
fun TunerPreset.presentation(status: TunerStatus?): PresetPresentation {
    val selected = status?.preset == number
    val frequencyTitle = title.takeIf { it.matches(Regex("(?i)(FM|AM)\\s+[0-9.,]+\\s*(MHz|kHz)?")) }
    val named = title.takeUnless { it == "Preset $number" || frequencyTitle != null || it.isBlank() }
    // Live RDS belongs only to the current preset; never cache it onto other stations.
    return PresetPresentation(number, if (selected) status.nowPlaying.station ?: named else named,
        frequencyTitle ?: if (selected) status.frequency?.display else null, selected)
}
