package com.bugsjkeeee.tempo.timer

/** Упражнение для подсказки на экране таймера. */
data class GuideItem(val name: String, val dose: String = "")

/**
 * Что делать во время таймера: упражнения тренировки или разминки.
 * [rotate] — каждый раунд следующее упражнение по кругу (разминка, чередующиеся интервалы);
 * иначе весь список выполняется внутри раунда (For Time, AMRAP, EMOM).
 */
data class TimerGuide(
    val title: String,
    val items: List<GuideItem>,
    val rotate: Boolean = false,
    val subtitle: String = "",
    val note: String = "",
) {
    /** Упражнение раунда [round] (с 1) при чередовании. */
    fun itemForRound(round: Int): GuideItem? =
        if (!rotate || items.isEmpty()) null else items[(round - 1).coerceAtLeast(0) % items.size]
}

private val roundMarks = Regex("""—\s*\d-я(\s+мин)?|(—\s*)?(нечётные|чётные)(\s+(минуты|раунды))?|раунды\s+[\d,\s]+""")

/** Дозировка без пометок о раундах («12 — нечётные минуты» → «12», «раунды 1, 5» → «»). */
fun rotationDose(dose: String): String =
    dose.replace(roundMarks, "").replace(Regex("""\s+,"""), ",").trim().trim(',', '—', ' ')
