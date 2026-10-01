package com.bugsjkeeee.tempo.content

import kotlin.random.Random

/** Фильтры рандомайзера и раздела «База». Пустое значение — фильтр не задан. */
data class WorkoutFilters(
    val type: WorkoutType? = null,
    val muscle: Muscle? = null,
    val duration: DurationRange? = null,
    val format: WorkoutFormat? = null,
    val level: Level? = null,
    /** Доступное оборудование; пусто — любое. */
    val equipment: Set<Equipment> = emptySet(),
    val onlyFavorites: Boolean = false,
    val query: String = "",
) {
    val isEmpty: Boolean
        get() = type == null && muscle == null && duration == null && format == null && level == null &&
            equipment.isEmpty() && !onlyFavorites && query.isBlank()
}

/** Отметки пользователя по тренировке. */
data class WorkoutFlags(val favorite: Boolean = false, val hidden: Boolean = false)

/**
 * Подходит ли тренировка под фильтры.
 * Оборудование работает по правилу «только отмеченное»: всё нужное оборудование должно быть отмечено,
 * собственный вес подходит всегда.
 */
fun Workout.matches(f: WorkoutFilters, flags: WorkoutFlags): Boolean {
    if (f.type != null && type != f.type) return false
    if (f.muscle != null && f.muscle !in muscles) return false
    if (f.duration != null && durationMin !in f.duration.range) return false
    if (f.format != null && format != f.format) return false
    if (f.level != null && level != f.level.value) return false
    if (f.equipment.isNotEmpty() && !equipment.all { it == Equipment.BODYWEIGHT || it in f.equipment }) return false
    if (f.onlyFavorites && !flags.favorite) return false
    if (f.query.isNotBlank() && !name.contains(f.query.trim(), ignoreCase = true)) return false
    return true
}

/**
 * Кандидаты рандомайзера: подходят под фильтры, не скрыты и не выполнялись за последние дни.
 * [recentlyDone] — id тренировок из журнала за последние 14 дней.
 */
fun randomCandidates(
    workouts: List<Workout>,
    filters: WorkoutFilters,
    flags: Map<String, WorkoutFlags>,
    recentlyDone: Set<String>,
): List<Workout> = workouts.filter { w ->
    val f = flags[w.id] ?: WorkoutFlags()
    !f.hidden && w.id !in recentlyDone && w.matches(filters, f)
}

/** Случайная тренировка, по возможности не совпадающая с текущей. */
fun pickRandom(candidates: List<Workout>, currentId: String?, random: Random = Random.Default): Workout? {
    if (candidates.isEmpty()) return null
    val others = candidates.filter { it.id != currentId }
    return (others.ifEmpty { candidates }).random(random)
}

/** Оборудование, группы мышц и тип для своей тренировки определяются по упражнениям. */
fun deriveWorkout(
    id: String,
    name: String,
    format: WorkoutFormat,
    level: Int,
    items: List<WorkoutItem>,
    timer: TimerSpec?,
    description: String,
    exercises: Map<String, Exercise>,
): Workout {
    val used = items.mapNotNull { exercises[it.exercise] }
    val muscles = used.flatMap { it.muscles }.distinct().toMutableList()
    val type = if (format == WorkoutFormat.SETS) WorkoutType.STRENGTH else WorkoutType.FUNCTIONAL
    if (type == WorkoutType.FUNCTIONAL && Muscle.FULL !in muscles) muscles += Muscle.FULL
    val duration = when (format) {
        WorkoutFormat.SETS -> (((items.sumOf { it.sets ?: 0 } * 2.5) / 5.0).let { kotlin.math.ceil(it) } * 5).toInt()
        WorkoutFormat.FOR_TIME -> (timer?.capSec ?: 0) / 60
        WorkoutFormat.AMRAP -> (timer?.durationSec ?: 0) / 60
        WorkoutFormat.EMOM -> ((timer?.intervalSec ?: 60) * (timer?.rounds ?: 0) + 59) / 60
        WorkoutFormat.TABATA -> (((timer?.workSec ?: 0) + (timer?.restSec ?: 0)) * (timer?.rounds ?: 0) + 59) / 60
    }
    return Workout(
        id = id,
        name = name,
        type = type,
        format = format,
        level = level,
        durationMin = duration,
        muscles = muscles,
        equipment = used.flatMap { it.equipment }.filter { it != Equipment.BODYWEIGHT }.distinct().sortedBy { it.ordinal },
        items = items,
        timer = timer,
        description = description,
        custom = true,
    )
}
