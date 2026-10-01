package com.bugsjkeeee.tempo.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FiltersTest {
    private fun w(id: String, eq: List<Equipment>, format: WorkoutFormat = WorkoutFormat.AMRAP, minutes: Int = 12, muscles: List<Muscle> = listOf(Muscle.FULL)) =
        Workout(id, "Тренировка $id", if (format == WorkoutFormat.SETS) WorkoutType.STRENGTH else WorkoutType.FUNCTIONAL, format, 2, minutes, muscles, eq, emptyList())

    private val barbell = w("b", listOf(Equipment.BARBELL))
    private val kbBar = w("k", listOf(Equipment.KETTLEBELL, Equipment.PULLUP_BAR))
    private val bodyweight = w("n", emptyList())

    @Test
    fun equipmentFilterAllowsOnlySelectedOrBodyweight() {
        val f = WorkoutFilters(equipment = setOf(Equipment.KETTLEBELL, Equipment.PULLUP_BAR))
        assertTrue(kbBar.matches(f, WorkoutFlags()))
        assertTrue(bodyweight.matches(f, WorkoutFlags()))
        assertFalse(barbell.matches(f, WorkoutFlags()))
        assertFalse(kbBar.matches(WorkoutFilters(equipment = setOf(Equipment.KETTLEBELL)), WorkoutFlags()))
    }

    @Test
    fun emptyFiltersMatchEverything() {
        listOf(barbell, kbBar, bodyweight).forEach { assertTrue(it.matches(WorkoutFilters(), WorkoutFlags())) }
    }

    @Test
    fun durationFormatAndFavorites() {
        val long = w("l", emptyList(), minutes = 40)
        assertTrue(long.matches(WorkoutFilters(duration = DurationRange.LONG), WorkoutFlags()))
        assertFalse(long.matches(WorkoutFilters(duration = DurationRange.SHORT), WorkoutFlags()))
        assertFalse(long.matches(WorkoutFilters(format = WorkoutFormat.EMOM), WorkoutFlags()))
        assertFalse(long.matches(WorkoutFilters(onlyFavorites = true), WorkoutFlags()))
        assertTrue(long.matches(WorkoutFilters(onlyFavorites = true), WorkoutFlags(favorite = true)))
    }

    @Test
    fun randomSkipsHiddenAndRecentAndPrefersAnotherWorkout() {
        val all = listOf(barbell, kbBar, bodyweight)
        val candidates = randomCandidates(all, WorkoutFilters(), mapOf("b" to WorkoutFlags(hidden = true)), setOf("n"))
        assertEquals(listOf(kbBar), candidates)
        val two = listOf(kbBar, bodyweight)
        repeat(20) { assertNotEquals("k", pickRandom(two, "k", Random(it))?.id) }
        assertEquals(null, pickRandom(emptyList(), null))
    }

    @Test
    fun customWorkoutDerivesEquipmentAndDuration() {
        val ex = mapOf(
            "pullup" to Exercise("pullup", "Подтягивания", listOf(Muscle.BACK), listOf(Equipment.PULLUP_BAR)),
            "pushup" to Exercise("pushup", "Отжимания", listOf(Muscle.CHEST), listOf(Equipment.BODYWEIGHT)),
        )
        val w = deriveWorkout("u1", "Своя", WorkoutFormat.SETS, 2,
            listOf(WorkoutItem("pullup", sets = 4, reps = 8), WorkoutItem("pushup", sets = 4, reps = 20)), null, "", ex)
        assertEquals(listOf(Equipment.PULLUP_BAR), w.equipment)
        assertEquals(WorkoutType.STRENGTH, w.type)
        assertEquals(20, w.durationMin)
        assertTrue(w.custom)
    }
}
