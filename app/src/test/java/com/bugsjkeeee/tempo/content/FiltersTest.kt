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
        assertTrue(long.matches(WorkoutFilters(durations = setOf(DurationRange.LONG)), WorkoutFlags()))
        assertFalse(long.matches(WorkoutFilters(durations = setOf(DurationRange.SHORT)), WorkoutFlags()))
        assertFalse(long.matches(WorkoutFilters(formats = setOf(WorkoutFormat.EMOM)), WorkoutFlags()))
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

    @Test
    fun multiSelectMatchesAnyOfValues() {
        val cardio = Workout("c", "Кардио", WorkoutType.CARDIO, WorkoutFormat.INTERVALS, 2, 30, listOf(Muscle.LEGS), listOf(Equipment.BIKE), emptyList())
        val func = w("f", emptyList(), minutes = 30)
        val strength = w("s", emptyList(), format = WorkoutFormat.SETS, minutes = 30)
        val f = WorkoutFilters(types = setOf(WorkoutType.CARDIO, WorkoutType.FUNCTIONAL), durations = setOf(DurationRange.MEDIUM))
        assertTrue(cardio.matches(f, WorkoutFlags()))
        assertTrue(func.matches(f, WorkoutFlags()))
        assertFalse(strength.matches(f, WorkoutFlags()))
        assertTrue(cardio.matches(WorkoutFilters(muscles = setOf(Muscle.BACK, Muscle.LEGS)), WorkoutFlags()))
        assertFalse(cardio.matches(WorkoutFilters(muscles = setOf(Muscle.BACK)), WorkoutFlags()))
    }

    @Test
    fun customWorkoutOnCardioMachinesIsCardio() {
        val ex = mapOf(
            "bike" to Exercise("bike", "Велотренажёр", listOf(Muscle.LEGS), listOf(Equipment.BIKE)),
            "row_erg" to Exercise("row_erg", "Гребля", listOf(Muscle.FULL), listOf(Equipment.ROWER)),
        )
        val w = deriveWorkout("u2", "Своё кардио", WorkoutFormat.STEADY, 1,
            listOf(WorkoutItem("bike", dose = "20 мин"), WorkoutItem("row_erg", dose = "10 мин")),
            TimerSpec(com.bugsjkeeee.tempo.timer.TimerMode.COUNTDOWN, durationSec = 1800), "", ex)
        assertEquals(WorkoutType.CARDIO, w.type)
        assertEquals(30, w.durationMin)
    }
}
