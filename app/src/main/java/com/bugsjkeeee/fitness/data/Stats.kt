package com.bugsjkeeee.fitness.data

import kotlin.math.roundToInt

/** Расчётный одноповторный максимум по формуле Эпли. */
fun estimatedOneRepMax(weight: Double, reps: Int): Double = when {
    reps <= 0 || weight <= 0.0 -> 0.0
    reps == 1 -> weight
    else -> weight * (1 + reps / 30.0)
}

/** Тоннаж — сумма «вес × повторения» по выполненным подходам. */
fun volume(sets: List<SetWithInfo>): Double = sets.filter { it.done }.sumOf { it.weight * it.reps }

/** Точка графика прогресса по одному упражнению за одну тренировку. */
data class ProgressPoint(
    val date: Long,
    val bestOneRepMax: Double,
    val maxWeight: Double,
    val volume: Double,
)

fun progressPoints(sets: List<SetWithInfo>): List<ProgressPoint> =
    sets.filter { it.done && it.finishedAt != null }
        .groupBy { it.workoutId }
        .map { (_, workoutSets) ->
            ProgressPoint(
                date = workoutSets.first().finishedAt!!,
                bestOneRepMax = workoutSets.maxOf { estimatedOneRepMax(it.weight, it.reps) },
                maxWeight = workoutSets.maxOf { it.weight },
                volume = volume(workoutSets),
            )
        }
        .sortedBy { it.date }

/** Форматирует вес без лишних нулей: 80.0 → «80», 82.5 → «82.5». */
fun formatWeight(value: Double): String {
    val rounded = (value * 100).roundToInt() / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
