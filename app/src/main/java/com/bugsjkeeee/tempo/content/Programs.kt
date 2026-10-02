package com.bugsjkeeee.tempo.content

import com.bugsjkeeee.tempo.data.JournalEntry

/**
 * Следующий день программы, которой пользовались последней:
 * после последнего выполненного дня — следующий по кругу.
 */
fun nextProgramWorkout(workouts: List<Workout>, entries: List<JournalEntry>): Workout? {
    val byId = workouts.associateBy { it.id }
    val last = entries.sortedByDescending { it.date }
        .firstNotNullOfOrNull { e -> e.workoutId?.let(byId::get)?.takeIf { it.program != null } }
        ?: return null
    val days = workouts.filter { it.program == last.program }.sortedBy { it.programDay }
    if (days.isEmpty()) return null
    val next = (last.programDay ?: 0) % days.size + 1
    return days.firstOrNull { it.programDay == next }
}

/** Разминка перед тренировкой: упражнения по 45 с работы и 15 с на переход. */
data class Warmup(val items: List<String>, val note: String? = null) {
    val workSec: Int get() = 45
    val restSec: Int get() = 15
    val minutes: Int get() = items.size
}

fun warmupFor(workout: Workout): Warmup = when (workout.type) {
    WorkoutType.STRENGTH -> Warmup(
        listOf(
            "Лёгкое кардио: бег на месте или тренажёр",
            "Вращения в плечах, локтях и кистях",
            "Вращения тазом, коленями и стопами",
            "Приседания без веса",
            "Наклоны с прямой спиной без веса",
            "Отжимания от пола в лёгком темпе",
        ),
        "Затем 1–2 разминочных подхода первого упражнения с 40–60 % рабочего веса.",
    )
    WorkoutType.CARDIO -> Warmup(
        listOf(
            "Лёгкий темп на тренажёре",
            "Махи ногами вперёд-назад",
            "Выпады с поворотом корпуса",
            "Подъёмы на носки",
            "Ускорение 20–30 с, затем легко",
        ),
    )
    WorkoutType.FUNCTIONAL -> Warmup(
        listOf(
            "Прыжки «звёздочка»",
            "Вращения руками и плечами",
            "Приседания без веса",
            "Отжимания от пола в лёгком темпе",
            "Выпады с поворотом корпуса",
            "Скалолаз",
        ),
        "Перед штангой — пара подходов основного движения с пустым грифом.",
    )
}
