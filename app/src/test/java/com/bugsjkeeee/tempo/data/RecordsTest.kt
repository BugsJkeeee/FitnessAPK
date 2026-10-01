package com.bugsjkeeee.tempo.data

import com.bugsjkeeee.tempo.content.WorkoutFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RecordsTest {
    private fun day(d: String) = LocalDate.parse(d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun fran(id: Long, date: String, ms: Long) =
        JournalEntry(id, day(date), "f001", "Фрэн", WorkoutFormat.FOR_TIME, resultTimeMs = ms)

    private fun bench(id: Long, date: String, vararg sets: Pair<Double, Int>) = JournalEntry(
        id, day(date), "s001", "Грудь", WorkoutFormat.SETS,
        sets = sets.mapIndexed { i, (w, r) -> JournalSet("bench_press", i + 1, w, r) },
    )

    @Test
    fun complexRecordNeedsTwoAttemptsAndPicksBestTime() {
        assertTrue(complexRecords(listOf(fran(1, "2026-01-01", 400_000))).isEmpty())
        val r = complexRecords(listOf(fran(1, "2026-01-01", 400_000), fran(2, "2026-02-01", 350_000))).single()
        assertEquals(2L, r.best.id)
        assertEquals(2, r.attempts.size)
    }

    @Test
    fun exerciseRecordUsesMaxWeightAndBestSet() {
        val r = exerciseRecords(listOf(bench(1, "2026-01-01", 80.0 to 3, 70.0 to 10), bench(2, "2026-01-08", 85.0 to 1))).single()
        assertEquals(85.0, r.maxWeight, 0.01)
        assertEquals(70.0, r.bestWeight, 0.01) // 70×10 ≈ 93 кг расчётного максимума — лучше 85×1
        assertEquals(2, r.history.size)
    }

    @Test
    fun newRecordDetection() {
        val prev = listOf(fran(1, "2026-01-01", 400_000), bench(2, "2026-01-01", 80.0 to 5))
        assertTrue(isNewRecord(fran(3, "2026-02-01", 390_000), prev))
        assertFalse(isNewRecord(fran(3, "2026-02-01", 410_000), prev))
        assertTrue(isNewRecord(bench(4, "2026-02-01", 82.5 to 3), prev))
        assertFalse(isNewRecord(bench(4, "2026-02-01", 80.0 to 8), prev))
        // Первая попытка рекордом не считается.
        assertFalse(isNewRecord(fran(5, "2026-02-01", 300_000), emptyList()))
    }

    @Test
    fun regularityCountsWeekMonthAndStreak() {
        val dates = listOf("2026-09-14", "2026-09-22", "2026-09-29", "2026-10-01").map(::day)
        val r = regularity(dates, LocalDate.parse("2026-10-01"), ZoneOffset.UTC)
        assertEquals(2, r.week) // неделя с 28 сентября
        assertEquals(1, r.month)
        assertEquals(3, r.streakWeeks)
    }

    @Test
    fun trendIsMovingAverage() {
        val t = weightTrend(listOf(1L to 80.0, 2L to 82.0, 3L to 84.0), window = 2)
        assertEquals(listOf(80.0, 81.0, 83.0), t.map { it.second })
    }
}
