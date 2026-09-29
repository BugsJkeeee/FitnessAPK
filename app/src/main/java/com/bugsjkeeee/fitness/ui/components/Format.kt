package com.bugsjkeeee.fitness.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ruLocale = Locale("ru")

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy, EE", ruLocale).format(Date(millis))

fun formatShortDate(millis: Long): String =
    SimpleDateFormat("dd.MM", ruLocale).format(Date(millis))

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("d MMM, HH:mm", ruLocale).format(Date(millis))

/** 3725 000 мс → «1:02:05», 125 000 мс → «2:05». */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Тоннаж в читаемом виде: 850 → «850 кг», 12 400 → «12,4 т». */
fun formatVolume(kg: Double): String =
    if (kg >= 10_000) "%.1f т".format(ruLocale, kg / 1000) else "${kg.toLong()} кг"

/** Разбирает число, введённое с запятой или точкой. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
