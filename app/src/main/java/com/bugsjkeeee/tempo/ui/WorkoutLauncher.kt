package com.bugsjkeeee.tempo.ui

import com.bugsjkeeee.tempo.timer.rotationDose
import com.bugsjkeeee.tempo.timer.GuideItem
import com.bugsjkeeee.tempo.timer.TimerGuide
import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.content.warmupFor
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.content.WorkoutRef
import kotlinx.coroutines.flow.first

/**
 * Запуск тренировки кнопкой «Начать»: функциональная — таймер с её параметрами,
 * силовая — экран исполнения. Возвращает маршрут, который нужно открыть.
 */
/** Таймер разминки перед тренировкой: интервалы по числу упражнений разминки. */
suspend fun launchWarmup(app: TempoApp, workout: Workout): String {
    val w = warmupFor(workout)
    val base = app.settingsRepository.timerSettings.first()
    val guide = TimerGuide("Разминка", w.items.map { GuideItem(it) }, rotate = true, note = w.note.orEmpty())
    app.timerController.start(TimerMode.INTERVALS, base.copy(workSec = w.workSec, restSec = w.restSec, intervalRounds = w.items.size), guide = guide)
    return Routes.TIMER_RUN
}

suspend fun launchWorkout(app: TempoApp, workout: Workout): String {
    val spec = workout.timer ?: return Routes.execute(workout.id)
    val settings = spec.applyTo(app.settingsRepository.timerSettings.first())
    app.timerController.start(spec.mode, settings, WorkoutRef(workout.id, workout.name, workout.format), workoutGuide(app, workout))
    return Routes.TIMER_RUN
}

/** Список упражнений тренировки для экрана таймера. */
private suspend fun workoutGuide(app: TempoApp, workout: Workout): TimerGuide {
    val exercises = app.contentRepository.exerciseMap()
    val items = workout.items.map { item ->
        val dose = item.dose.orEmpty()
        GuideItem(exercises[item.exercise]?.name ?: item.exercise, if (workout.rotation) rotationDose(dose) else dose)
    }
    val subtitle = listOfNotNull(workout.format.title, "~${workout.durationMin} мин").joinToString(" · ")
    return TimerGuide(workout.name, items, rotate = workout.rotation, subtitle = subtitle, note = workout.description)
}
