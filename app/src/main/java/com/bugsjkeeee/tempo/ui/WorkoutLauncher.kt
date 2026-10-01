package com.bugsjkeeee.tempo.ui

import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.content.WorkoutRef
import kotlinx.coroutines.flow.first

/**
 * Запуск тренировки кнопкой «Начать»: функциональная — таймер с её параметрами,
 * силовая — экран исполнения. Возвращает маршрут, который нужно открыть.
 */
suspend fun launchWorkout(app: TempoApp, workout: Workout): String {
    val spec = workout.timer ?: return Routes.execute(workout.id)
    val settings = spec.applyTo(app.settingsRepository.timerSettings.first())
    app.timerController.start(spec.mode, settings, WorkoutRef(workout.id, workout.name, workout.format))
    return Routes.TIMER_RUN
}
