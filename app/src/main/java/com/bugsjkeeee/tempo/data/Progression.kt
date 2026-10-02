package com.bugsjkeeee.tempo.data

import kotlin.math.roundToInt

/** Подходы упражнения из последней тренировки, где оно выполнялось. */
data class LastPerformance(val date: Long, val sets: List<JournalSet>)

fun lastPerformance(entries: List<JournalEntry>, exerciseId: String): LastPerformance? =
    entries
        .filter { e -> e.sets.any { it.exerciseId == exerciseId && it.reps != null } }
        .maxByOrNull { it.date }
        ?.let { e -> LastPerformance(e.date, e.sets.filter { it.exerciseId == exerciseId }.sortedBy { it.setIndex }) }

private const val STEP = 2.5

private fun roundToStep(weight: Double) = (weight / STEP).roundToInt() * STEP

/**
 * Рекомендация рабочего веса по двум последним тренировкам с этим упражнением:
 * все подходы с рабочим весом сделаны на целевые повторы — +2,5 кг;
 * не добрал — тот же вес; не добрал два раза подряд — разгрузка −10 %.
 * Без целевых повторов или без веса рекомендации нет.
 */
fun suggestWeight(entries: List<JournalEntry>, exerciseId: String, targetReps: Int?): Double? {
    if (targetReps == null || targetReps <= 0) return null
    val sessions = entries
        .map { e -> e.date to e.sets.filter { it.exerciseId == exerciseId && it.weight != null && it.reps != null } }
        .filter { (_, sets) -> sets.isNotEmpty() }
        .sortedByDescending { it.first }
        .take(2)
        .map { it.second }
    val last = sessions.firstOrNull() ?: return null
    val work = last.maxOf { it.weight!! }
    if (work <= 0) return null
    fun hit(sets: List<JournalSet>): Boolean {
        val top = sets.maxOf { it.weight!! }
        return sets.filter { it.weight == top }.all { it.reps!! >= targetReps }
    }
    return when {
        hit(last) -> work + STEP
        sessions.size == 2 && !hit(sessions[1]) -> roundToStep(work * 0.9).coerceAtLeast(STEP)
        else -> work
    }
}
