package com.bugsjkeeee.tempo.timer

/**
 * Состояние одного запуска таймера. Не зависит от Android: время передаётся снаружи
 * (монотонные часы), поэтому логика проверяется юнит-тестами.
 */
class TimerRun(val plan: TimerPlan, startedAt: Long) {
    private var accumulatedMs = 0L
    private var resumedAt: Long? = startedAt

    /** Прошедшее время на момент последнего опроса — от него считаются события. */
    private var lastElapsedMs = 0L

    /** Отметки кругов (секундомер) или раундов (AMRAP): общее время без подготовки на момент нажатия. */
    val laps = mutableListOf<Long>()

    /** Время ручного финиша (For Time, секундомер) или null. */
    var manualFinishMs: Long? = null
        private set

    val isPaused: Boolean get() = resumedAt == null && !isFinished
    val isFinished: Boolean get() = manualFinishMs != null || (plan.totalMs != null && lastElapsedMs >= plan.totalMs)

    fun elapsed(now: Long): Long {
        manualFinishMs?.let { return it }
        val running = resumedAt?.let { now - it } ?: 0L
        val e = accumulatedMs + running
        return plan.totalMs?.let { minOf(e, it) } ?: e
    }

    /** Продвигает время и возвращает события, произошедшие с прошлого опроса. */
    fun poll(now: Long): List<TimerEvent> {
        val e = elapsed(now)
        val events = plan.eventsBetween(lastElapsedMs, e)
        lastElapsedMs = e
        return events
    }

    fun pause(now: Long) {
        val r = resumedAt ?: return
        accumulatedMs += now - r
        resumedAt = null
    }

    fun resume(now: Long) {
        if (resumedAt == null && !isFinished) resumedAt = now
    }

    /**
     * Пропускает текущий отрезок: время переносится на начало следующего.
     * Возвращает событие начала следующего отрезка (или окончания), чтобы прозвучал нужный сигнал без отсчёта 3-2-1.
     */
    fun skip(now: Long): TimerEvent? {
        val e = elapsed(now)
        val pos = plan.positionAt(e)
        if (pos.finished) return null
        val duration = plan.segments[pos.index].durationMs ?: return null
        val target = plan.starts[pos.index] + duration
        accumulatedMs += target - e
        lastElapsedMs = target
        return if (pos.index == plan.segments.lastIndex) TimerEvent.Finish else plan.startEvent(pos.index + 1)
    }

    fun lap(now: Long) {
        if (isPaused || isFinished) return
        val t = elapsed(now) - plan.prepMs
        if (t > 0) laps += t
    }

    /** Ручная остановка: фиксирует время, после этого таймер считается завершённым. */
    fun finish(now: Long) {
        if (manualFinishMs == null) manualFinishMs = elapsed(now)
        resumedAt = null
    }
}
