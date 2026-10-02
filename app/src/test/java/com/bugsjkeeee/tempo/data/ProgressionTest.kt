package com.bugsjkeeee.tempo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionTest {
    private fun entry(date: Long, vararg sets: Pair<Double, Int>) =
        JournalEntry(date = date, title = "T", sets = sets.mapIndexed { i, (w, r) -> JournalSet("squat", i + 1, w, r) })

    @Test
    fun allTargetRepsAddsStep() {
        val e = listOf(entry(1, 60.0 to 10, 60.0 to 10, 60.0 to 10))
        assertEquals(62.5, suggestWeight(e, "squat", 10)!!, 0.001)
    }

    @Test
    fun missedOnceKeepsWeight() {
        val e = listOf(entry(1, 60.0 to 10, 60.0 to 10), entry(2, 62.5 to 10, 62.5 to 8))
        assertEquals(62.5, suggestWeight(e, "squat", 10)!!, 0.001)
    }

    @Test
    fun missedTwiceDeloads() {
        val e = listOf(entry(1, 62.5 to 9), entry(2, 62.5 to 8))
        assertEquals(57.5, suggestWeight(e, "squat", 10)!!, 0.001)
    }

    @Test
    fun noTargetOrNoHistoryNoSuggestion() {
        assertNull(suggestWeight(emptyList(), "squat", 10))
        assertNull(suggestWeight(listOf(entry(1, 60.0 to 10)), "squat", null))
    }

    @Test
    fun lastPerformanceTakesLatestEntry() {
        val e = listOf(entry(1, 50.0 to 10), entry(5, 60.0 to 8, 60.0 to 6))
        assertEquals(listOf(8, 6), lastPerformance(e, "squat")!!.sets.map { it.reps })
    }
}
