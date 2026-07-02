package com.example.jugglersettingtool.utils

import java.util.Locale

private val DecimalLocale = Locale.US
private val YenLocale = Locale.JAPAN

fun formatDecimal(value: Double, digits: Int): String =
    String.format(DecimalLocale, "%.${digits}f", value)

fun formatHitRatio(games: Int, hits: Int, digits: Int, emptyText: String = "-"): String =
    if (games > 0 && hits > 0) {
        "1/${formatDecimal(games.toDouble() / hits, digits)}"
    } else {
        emptyText
    }

fun formatPercent(value: Double, digits: Int = 1): String =
    "${formatDecimal(value, digits)}%"

fun formatYen(value: Double, signed: Boolean = false): String {
    val rounded = value.toInt()
    val sign = if (signed && rounded >= 0) "+" else ""
    return "$sign${String.format(YenLocale, "%,d", rounded)} 円"
}
