package com.bugsjkeeee.tempo.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerModelTest {

    private val settings = TimerSettings(workSec = 20, restSec = 40, intervalRounds = 3, emomIntervalSec = 60, emomRounds = 2)

    @Test
    fun intervalsPlanAlternatesWorkAndRestWithoutFinalRest() {
        val plan = buildPlan(TimerMode.INTERVALS, settings, prepSec = 10)
        assertEquals(
            listOf(Phase.PREP, Phase.WORK, Phase.REST, Phase.WORK, Phase.REST, Phase.WORK),
            plan.segments.map { it.phase },
        )
        assertEquals(10_000L + 3 * 20_000L + 2 * 40_000L, plan.totalMs)
        assertTrue(plan.segments.last().isLastRound)
    }

    @Test
    fun stopwatchAndForTimeWithoutCapAreOpenEnded() {
        assertNull(buildPlan(TimerMode.STOPWATCH, settings, 0).totalMs)
        assertNull(buildPlan(TimerMode.FOR_TIME, settings.copy(forTimeCapSec = 0), 0).totalMs)
        assertEquals(600_000L, buildPlan(TimerMode.FOR_TIME, settings.copy(forTimeCapSec = 600), 0).totalMs)
    }

    @Test
    fun customPlanInsertsRestBetweenBlocks() {
        val s = settings.copy(
            customBlocks = listOf(
                Block(listOf(Interval(Phase.WORK, 300)), 1),
                Block(listOf(Interval(Phase.WORK, 40), Interval(Phase.REST, 20)), 2),
            ),
            customRestBetweenBlocksSec = 120,
        )
        val plan = buildPlan(TimerMode.CUSTOM, s, 0)
        assertEquals(
            listOf(300, 120, 40, 20, 40, 20),
            plan.segments.map { (it.durationMs!! / 1000).toInt() },
        )
        assertEquals(2, plan.segments[2].block)
        assertEquals(2, plan.segments[2].totalBlocks)
    }

    @Test
    fun positionFindsSegmentAndFinish() {
        val plan = buildPlan(TimerMode.EMOM, settings, prepSec = 10)
        assertEquals(Position(0, 5_000, false), plan.positionAt(5_000))
        assertEquals(Position(1, 0, false), plan.positionAt(10_000))
        assertEquals(Position(2, 30_000, false), plan.positionAt(100_000))
        assertTrue(plan.positionAt(130_000).finished)
    }

    @Test
    fun countdownAndStartEventsAreEmittedInOrder() {
        val plan = buildPlan(TimerMode.INTERVALS, settings, prepSec = 10)
        val events = plan.eventsBetween(6_500, 10_000)
        assertEquals(
            listOf(
                TimerEvent.Countdown(3),
                TimerEvent.Countdown(2),
                TimerEvent.Countdown(1),
                TimerEvent.WorkStart(lastRound = false),
            ),
            events,
        )
        assertEquals(listOf(TimerEvent.RestStart), plan.eventsBetween(29_500, 30_000))
        val total = plan.totalMs!!
        assertEquals(TimerEvent.Finish, plan.eventsBetween(total - 10, total).last())
    }

    @Test
    fun lastRoundIsAnnounced() {
        val plan = buildPlan(TimerMode.EMOM, settings, prepSec = 0)
        assertEquals(listOf(TimerEvent.WorkStart(lastRound = true)), plan.eventsBetween(59_999, 60_000))
    }

    @Test
    fun shortSegmentsDoNotCountDownFromPreviousSegment() {
        val s = settings.copy(workSec = 2, restSec = 2, intervalRounds = 2)
        val plan = buildPlan(TimerMode.INTERVALS, s, prepSec = 0)
        // Отрезок 2 с: отсчёт только «2» и «1» внутри него.
        val countdowns = plan.eventsBetween(-1, 2_000).filterIsInstance<TimerEvent.Countdown>()
        assertEquals(listOf(2, 1), countdowns.map { it.secondsLeft })
    }

    @Test
    fun pauseStopsTimeAndSkipJumpsToNextSegment() {
        val plan = buildPlan(TimerMode.INTERVALS, settings, prepSec = 0)
        val run = TimerRun(plan, startedAt = 1_000)
        run.poll(6_000)
        run.pause(6_000)
        assertEquals(5_000, run.elapsed(50_000))
        run.resume(50_000)
        assertEquals(6_000, run.elapsed(51_000))
        assertEquals(TimerEvent.RestStart, run.skip(51_000))
        assertEquals(20_000, run.elapsed(51_000))
        // После пропуска отсчёт 3-2-1 предыдущего отрезка не повторяется.
        assertTrue(run.poll(51_500).isEmpty())
    }

    @Test
    fun runFinishesAtPlanEndAndOnManualFinish() {
        val plan = buildPlan(TimerMode.COUNTDOWN, settings.copy(countdownSec = 60), prepSec = 0)
        val run = TimerRun(plan, 0)
        assertFalse(run.isFinished)
        assertEquals(TimerEvent.Finish, run.poll(70_000).last())
        assertTrue(run.isFinished)
        assertEquals(60_000, run.elapsed(90_000))

        val stopwatch = TimerRun(buildPlan(TimerMode.STOPWATCH, settings, 0), 0)
        stopwatch.lap(10_000)
        stopwatch.lap(25_000)
        stopwatch.finish(30_000)
        assertTrue(stopwatch.isFinished)
        assertEquals(listOf(10_000L, 25_000L), stopwatch.laps)
        assertEquals(30_000, stopwatch.elapsed(99_000))
    }

    @Test
    fun blockCodecRoundTrips() {
        val blocks = TimerSettings.DEFAULT_CUSTOM
        assertEquals(blocks, BlockCodec.decode(BlockCodec.encode(blocks)))
        assertNull(BlockCodec.decode("мусор"))
    }
}
