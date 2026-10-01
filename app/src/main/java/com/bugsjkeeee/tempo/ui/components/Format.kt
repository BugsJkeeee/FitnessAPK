package com.bugsjkeeee.tempo.ui.components

/** 65 000 мс → «01:05», 3 725 000 мс → «1:02:05». */
fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Время с десятыми для результатов и кругов: «01:05.3». */
fun formatPrecise(ms: Long): String {
    val tenths = (ms.coerceAtLeast(0) % 1000) / 100
    return formatClock(ms) + "." + tenths
}

/** Оставшееся время округляется вверх, чтобы «00:00» показывалось только в момент окончания. */
fun formatRemaining(ms: Long): String = formatClock(((ms + 999) / 1000) * 1000)

private val ruLocale = java.util.Locale("ru")

/** 80.0 → «80», 82.5 → «82,5». */
fun formatWeight(value: Double): String {
    val rounded = Math.round(value * 10) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString().replace('.', ',')
}

fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

fun formatDate(ms: Long): String =
    java.text.SimpleDateFormat("d MMMM yyyy", ruLocale).format(java.util.Date(ms))

fun formatDateShort(ms: Long): String =
    java.text.SimpleDateFormat("d MMM", ruLocale).format(java.util.Date(ms))

fun formatDateTime(ms: Long): String =
    java.text.SimpleDateFormat("d MMMM yyyy, HH:mm", ruLocale).format(java.util.Date(ms))

fun formatEpochDay(day: Long): String =
    java.time.LocalDate.ofEpochDay(day).format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale))

/** «5 тренировок», «1 тренировка», «2 тренировки». */
fun plural(n: Int, one: String, few: String, many: String): String {
    val n100 = n % 100
    val n10 = n % 10
    val word = when {
        n100 in 11..14 -> many
        n10 == 1 -> one
        n10 in 2..4 -> few
        else -> many
    }
    return "$n $word"
}
