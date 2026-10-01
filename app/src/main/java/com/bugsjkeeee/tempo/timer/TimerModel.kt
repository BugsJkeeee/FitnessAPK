package com.bugsjkeeee.tempo.timer

/** Фаза интервала; от неё зависит цвет экрана таймера и звук начала. */
enum class Phase { PREP, WORK, REST }

enum class TimerMode(val title: String) {
    STOPWATCH("Секундомер"),
    COUNTDOWN("Таймер"),
    FOR_TIME("For Time"),
    AMRAP("AMRAP"),
    EMOM("EMOM"),
    INTERVALS("Табата"),
    CUSTOM("Свой"),
}

/** Интервал своего сценария: фаза (работа или отдых) и длительность. */
data class Interval(val phase: Phase, val seconds: Int)

/** Блок своего сценария: последовательность интервалов, повторяемая [repeats] раз. */
data class Block(val intervals: List<Interval>, val repeats: Int)

/** Последние настройки всех режимов; каждый режим помнит свои значения независимо. */
data class TimerSettings(
    val countdownSec: Int = 5 * 60,
    val forTimeCapSec: Int = 0,
    val amrapSec: Int = 12 * 60,
    val emomIntervalSec: Int = 60,
    val emomRounds: Int = 10,
    val workSec: Int = 20,
    val restSec: Int = 10,
    val intervalRounds: Int = 8,
    val customBlocks: List<Block> = DEFAULT_CUSTOM,
    val customRestBetweenBlocksSec: Int = 120,
) {
    companion object {
        val DEFAULT_CUSTOM = listOf(
            Block(listOf(Interval(Phase.WORK, 5 * 60)), 1),
            Block(listOf(Interval(Phase.WORK, 40), Interval(Phase.REST, 20)), 8),
            Block(listOf(Interval(Phase.WORK, 40), Interval(Phase.REST, 20)), 8),
            Block(listOf(Interval(Phase.WORK, 40), Interval(Phase.REST, 20)), 8),
        )
    }
}

/**
 * Отрезок плана тренировки.
 * [durationMs] == null — открытый отрезок без конца (секундомер, For Time без лимита).
 */
data class Segment(
    val phase: Phase,
    val durationMs: Long?,
    val round: Int = 0,
    val totalRounds: Int = 0,
    val block: Int = 0,
    val totalBlocks: Int = 0,
) {
    val isLastRound: Boolean get() = totalRounds > 1 && round == totalRounds && (totalBlocks <= 1 || block == totalBlocks)
}

data class TimerPlan(val mode: TimerMode, val segments: List<Segment>) {
    /** Момент начала каждого отрезка от старта, мс. */
    val starts: List<Long> = segments.runningFold(0L) { acc, s -> acc + (s.durationMs ?: 0L) }.dropLast(1)

    /** Полная длительность или null, если в плане есть открытый отрезок. */
    val totalMs: Long? = if (segments.any { it.durationMs == null }) null else segments.sumOf { it.durationMs!! }

    /** Длительность подготовки (первый отрезок PREP), мс. */
    val prepMs: Long = segments.firstOrNull()?.takeIf { it.phase == Phase.PREP }?.durationMs ?: 0L
}

/** Собирает план отрезков для режима из настроек. */
fun buildPlan(mode: TimerMode, s: TimerSettings, prepSec: Int): TimerPlan {
    val segments = mutableListOf<Segment>()
    if (prepSec > 0) segments += Segment(Phase.PREP, prepSec * 1000L)
    fun work(sec: Int, round: Int = 0, total: Int = 0) =
        Segment(Phase.WORK, sec * 1000L, round, total)

    when (mode) {
        TimerMode.STOPWATCH -> segments += Segment(Phase.WORK, null)
        TimerMode.COUNTDOWN -> segments += work(s.countdownSec)
        TimerMode.FOR_TIME -> segments += Segment(Phase.WORK, s.forTimeCapSec.takeIf { it > 0 }?.times(1000L))
        TimerMode.AMRAP -> segments += work(s.amrapSec)
        TimerMode.EMOM -> repeat(s.emomRounds) { segments += work(s.emomIntervalSec, it + 1, s.emomRounds) }
        TimerMode.INTERVALS -> repeat(s.intervalRounds) { i ->
            segments += work(s.workSec, i + 1, s.intervalRounds)
            // После последнего раунда отдых не нужен — тренировка заканчивается.
            if (i < s.intervalRounds - 1 && s.restSec > 0) {
                segments += Segment(Phase.REST, s.restSec * 1000L, i + 1, s.intervalRounds)
            }
        }
        TimerMode.CUSTOM -> {
            val blocks = s.customBlocks.filter { b -> b.repeats > 0 && b.intervals.any { it.seconds > 0 } }
            blocks.forEachIndexed { b, block ->
                if (b > 0 && s.customRestBetweenBlocksSec > 0) {
                    segments += Segment(Phase.REST, s.customRestBetweenBlocksSec * 1000L, block = b, totalBlocks = blocks.size)
                }
                repeat(block.repeats) { r ->
                    block.intervals.filter { it.seconds > 0 }.forEach { interval ->
                        segments += Segment(interval.phase, interval.seconds * 1000L, r + 1, block.repeats, b + 1, blocks.size)
                    }
                }
            }
        }
    }
    return TimerPlan(mode, segments)
}

/** Положение внутри плана для заданного прошедшего времени. */
data class Position(
    val index: Int,
    val segmentElapsedMs: Long,
    val finished: Boolean,
)

fun TimerPlan.positionAt(elapsedMs: Long): Position {
    for (i in segments.indices) {
        val d = segments[i].durationMs ?: return Position(i, elapsedMs - starts[i], finished = false)
        if (elapsedMs < starts[i] + d) return Position(i, elapsedMs - starts[i], finished = false)
    }
    val last = segments.lastIndex
    return Position(last, segments[last].durationMs ?: 0L, finished = true)
}

sealed interface TimerEvent {
    /** Отсчёт перед сменой интервала: 3, 2, 1. */
    data class Countdown(val secondsLeft: Int) : TimerEvent
    data class WorkStart(val lastRound: Boolean) : TimerEvent
    data object RestStart : TimerEvent
    data object PrepStart : TimerEvent
    data object Finish : TimerEvent
}

/** Событие начала отрезка [index]. */
fun TimerPlan.startEvent(index: Int): TimerEvent {
    val s = segments[index]
    return when (s.phase) {
        Phase.PREP -> TimerEvent.PrepStart
        Phase.WORK -> TimerEvent.WorkStart(s.isLastRound)
        Phase.REST -> TimerEvent.RestStart
    }
}

/**
 * События, произошедшие на промежутке (from, to] прошедшего времени:
 * отсчёт 3-2-1 перед каждой границей, начала отрезков и окончание плана.
 */
fun TimerPlan.eventsBetween(fromMs: Long, toMs: Long): List<TimerEvent> {
    if (toMs <= fromMs) return emptyList()
    val events = mutableListOf<Pair<Long, TimerEvent>>()
    for (i in segments.indices) {
        val d = segments[i].durationMs ?: break
        val segStart = starts[i]
        val boundary = segStart + d
        for (k in 3 downTo 1) {
            val t = boundary - k * 1000L
            // Отсчёт звучит только внутри своего отрезка: короткие отрезки не «пищат» из предыдущего.
            if (t >= segStart && t > fromMs && t <= toMs) events += t to TimerEvent.Countdown(k)
        }
        if (boundary > fromMs && boundary <= toMs) {
            events += boundary to if (i == segments.lastIndex) TimerEvent.Finish else startEvent(i + 1)
        }
    }
    return events.sortedBy { it.first }.map { it.second }
}
