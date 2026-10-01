package com.bugsjkeeee.tempo.data

import com.bugsjkeeee.tempo.content.WorkoutFormat
import com.bugsjkeeee.tempo.timer.TimerMode
import kotlinx.serialization.Serializable

@Serializable
data class JournalSet(
    val exerciseId: String,
    val setIndex: Int,
    val weight: Double? = null,
    val reps: Int? = null,
)

/** Запись журнала: выполненная тренировка с результатом и подходами. */
@Serializable
data class JournalEntry(
    val id: Long = 0,
    /** Время тренировки, мс с 1970 года. */
    val date: Long,
    val workoutId: String? = null,
    val title: String,
    val format: WorkoutFormat? = null,
    val timerMode: TimerMode? = null,
    val durationMs: Long? = null,
    val resultTimeMs: Long? = null,
    val resultRounds: Int? = null,
    val note: String = "",
    val sets: List<JournalSet> = emptyList(),
) {
    /** Тоннаж — сумма «вес × повторы» по подходам с весом. */
    val volume: Double get() = sets.sumOf { (it.weight ?: 0.0) * (it.reps ?: 0) }
}

fun EntryWithSets.toDomain() = JournalEntry(
    id = entry.id,
    date = entry.date,
    workoutId = entry.workoutId,
    title = entry.title,
    format = entry.format?.let { runCatching { WorkoutFormat.valueOf(it) }.getOrNull() },
    timerMode = entry.timerMode?.let { runCatching { TimerMode.valueOf(it) }.getOrNull() },
    durationMs = entry.durationMs,
    resultTimeMs = entry.resultTimeMs,
    resultRounds = entry.resultRounds,
    note = entry.note,
    sets = sets.sortedWith(compareBy({ it.position }, { it.setIndex }))
        .map { JournalSet(it.exerciseId, it.setIndex, it.weight, it.reps) },
)

fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    date = date,
    workoutId = workoutId,
    title = title,
    format = format?.name,
    timerMode = timerMode?.name,
    durationMs = durationMs,
    resultTimeMs = resultTimeMs,
    resultRounds = resultRounds,
    note = note,
)

/** Подходы сохраняются с порядком упражнений, чтобы журнал показывал их в исходной последовательности. */
fun JournalEntry.setEntities(entryId: Long): List<JournalSetEntity> {
    val order = sets.map { it.exerciseId }.distinct()
    return sets.map { JournalSetEntity(0, entryId, order.indexOf(it.exerciseId), it.exerciseId, it.setIndex, it.weight, it.reps) }
}
