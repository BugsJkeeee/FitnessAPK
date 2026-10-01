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
