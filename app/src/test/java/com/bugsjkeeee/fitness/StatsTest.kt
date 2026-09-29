package com.bugsjkeeee.fitness

import com.bugsjkeeee.fitness.data.SetWithInfo
import com.bugsjkeeee.fitness.data.estimatedOneRepMax
import com.bugsjkeeee.fitness.data.formatWeight
import com.bugsjkeeee.fitness.data.progressPoints
import com.bugsjkeeee.fitness.data.volume
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {

    private fun set(workoutId: Long, weight: Double, reps: Int, done: Boolean = true, finishedAt: Long = workoutId * 1000) =
        SetWithInfo(
            id = 0, workoutId = workoutId, exerciseId = 1, exerciseName = "Жим", exerciseOrder = 0, setOrder = 0,
            weight = weight, reps = reps, done = done, finishedAt = finishedAt,
        )

    @Test
    fun oneRepMaxUsesEpleyFormula() {
        assertEquals(100.0, estimatedOneRepMax(100.0, 1), 0.001)
        assertEquals(120.0, estimatedOneRepMax(90.0, 10), 0.001)
        assertEquals(0.0, estimatedOneRepMax(100.0, 0), 0.001)
    }

    @Test
    fun volumeCountsOnlyDoneSets() {
        val sets = listOf(set(1, 100.0, 5), set(1, 100.0, 5, done = false), set(1, 50.0, 10))
        assertEquals(1000.0, volume(sets), 0.001)
    }

    @Test
    fun progressPointsGroupByWorkoutAndSortByDate() {
        val sets = listOf(set(2, 105.0, 3), set(1, 100.0, 5), set(1, 90.0, 8))
        val points = progressPoints(sets)
        assertEquals(2, points.size)
        assertEquals(1000L, points[0].date)
        assertEquals(100.0, points[0].maxWeight, 0.001)
        assertEquals(1220.0, points[0].volume, 0.001)
        assertEquals(105.0, points[1].maxWeight, 0.001)
    }

    @Test
    fun weightFormattingDropsTrailingZeros() {
        assertEquals("80", formatWeight(80.0))
        assertEquals("82.5", formatWeight(82.5))
    }
}
