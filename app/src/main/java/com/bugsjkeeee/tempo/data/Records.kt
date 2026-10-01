package com.bugsjkeeee.tempo.data

import com.bugsjkeeee.tempo.content.WorkoutFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Рекорд упражнения: максимальный вес и лучший подход «вес × повторы» по расчётному максимуму. */
data class ExerciseRecord(
    val exerciseId: String,
    val maxWeight: Double,
    val bestWeight: Double,
    val bestReps: Int,
    /** Максимальный вес за каждую тренировку: дата → вес. */
    val history: List<Pair<Long, Double>>,
)

/** Рекорд комплекса, выполненного больше одного раза. */
data class ComplexRecord(
    val workoutId: String,
    val title: String,
    val format: WorkoutFormat,
    val best: JournalEntry,
    val attempts: List<JournalEntry>,
)

/** Расчётный одноповторный максимум (формула Эпли) — для сравнения подходов с разными повторами. */
fun oneRepMax(weight: Double, reps: Int): Double = if (reps <= 1) weight else weight * (1 + reps / 30.0)

fun exerciseRecords(entries: List<JournalEntry>): List<ExerciseRecord> {
    val sets = entries.flatMap { e -> e.sets.filter { (it.weight ?: 0.0) > 0 && (it.reps ?: 0) > 0 }.map { e.date to it } }
    return sets.groupBy { it.second.exerciseId }.map { (id, list) ->
        val best = list.maxBy { oneRepMax(it.second.weight!!, it.second.reps!!) }.second
        ExerciseRecord(
            exerciseId = id,
            maxWeight = list.maxOf { it.second.weight!! },
            bestWeight = best.weight!!,
            bestReps = best.reps!!,
            history = list.groupBy { it.first }.map { (date, s) -> date to s.maxOf { it.second.weight!! } }.sortedBy { it.first },
        )
    }
}

private fun JournalEntry.hasResult() = when (format) {
    WorkoutFormat.FOR_TIME -> resultTimeMs != null
    WorkoutFormat.AMRAP -> resultRounds != null
    else -> false
}

/** Лучшая попытка: минимальное время (For Time) или максимум раундов (AMRAP). */
private fun List<JournalEntry>.best(format: WorkoutFormat): JournalEntry =
    if (format == WorkoutFormat.FOR_TIME) minBy { it.resultTimeMs!! } else maxBy { it.resultRounds!! }

fun complexRecords(entries: List<JournalEntry>): List<ComplexRecord> =
    entries.filter { it.workoutId != null && it.hasResult() }
        .groupBy { it.workoutId!! }
        .filter { it.value.size > 1 }
        .map { (id, list) ->
            val format = list.first().format!!
            ComplexRecord(id, list.first().title, format, list.best(format), list.sortedByDescending { it.date })
        }
        .sortedBy { it.title }

/**
 * Побит ли рекорд новой записью по сравнению с прежними.
 * Для комплекса — лучше прежнего времени или раундов; для силовой — вес больше прежнего максимума хотя бы в одном упражнении.
 * Если раньше результатов не было, рекордом это не считается.
 */
fun isNewRecord(new: JournalEntry, previous: List<JournalEntry>): Boolean {
    val others = previous.filter { it.id != new.id }
    if (new.workoutId != null && new.hasResult()) {
        val same = others.filter { it.workoutId == new.workoutId && it.hasResult() }
        if (same.isNotEmpty()) {
            return if (new.format == WorkoutFormat.FOR_TIME) {
                new.resultTimeMs!! < same.minOf { it.resultTimeMs!! }
            } else {
                new.resultRounds!! > same.maxOf { it.resultRounds!! }
            }
        }
    }
    val prevMax = exerciseRecords(others).associate { it.exerciseId to it.maxWeight }
    return new.sets.any { s ->
        val w = s.weight ?: return@any false
        val before = prevMax[s.exerciseId] ?: return@any false
        (s.reps ?: 0) > 0 && w > before
    }
}

/** Статистика регулярности: тренировок за неделю и месяц, серия недель подряд с тренировками. */
data class Regularity(val week: Int, val month: Int, val streakWeeks: Int)

fun regularity(dates: List<Long>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Regularity {
    val days = dates.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeks = days.map { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }.toSet()
    // Текущая неделя без тренировок ещё не прерывает серию.
    var cursor = if (weekStart in weeks) weekStart else weekStart.minusWeeks(1)
    var streak = 0
    while (cursor in weeks) {
        streak++
        cursor = cursor.minusWeeks(1)
    }
    return Regularity(
        week = days.count { !it.isBefore(weekStart) && !it.isAfter(today) },
        month = days.count { it.year == today.year && it.month == today.month },
        streakWeeks = streak,
    )
}

/** Сглаженный тренд веса: среднее по окну из [window] последних замеров для каждой точки. */
fun weightTrend(points: List<Pair<Long, Double>>, window: Int = 7): List<Pair<Long, Double>> {
    val sorted = points.sortedBy { it.first }
    return sorted.mapIndexed { i, (day, _) ->
        val from = maxOf(0, i - window + 1)
        day to sorted.subList(from, i + 1).map { it.second }.average()
    }
}
