package com.bugsjkeeee.tempo.content

import com.bugsjkeeee.tempo.data.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgramsTest {
    private fun day(id: String, n: Int) = Workout(
        id, "Сплит: день $n", WorkoutType.STRENGTH, WorkoutFormat.SETS, 2, 40, emptyList(), emptyList(), emptyList(),
        program = "split", programTitle = "Сплит", programDay = n, programDays = 3,
    )
    private val days = listOf(day("a", 1), day("b", 2), day("c", 3))
    private val other = Workout("x", "Другая", WorkoutType.FUNCTIONAL, WorkoutFormat.AMRAP, 2, 20, emptyList(), emptyList(), emptyList())

    @Test
    fun nextDayAfterLastDoneAndWrapsAround() {
        val e = listOf(JournalEntry(date = 1, workoutId = "a", title = ""), JournalEntry(date = 2, workoutId = "x", title = ""))
        assertEquals("b", nextProgramWorkout(days + other, e)?.id)
        assertEquals("a", nextProgramWorkout(days, listOf(JournalEntry(date = 5, workoutId = "c", title = "")))?.id)
    }

    @Test
    fun noProgramHistoryNoSuggestion() {
        assertNull(nextProgramWorkout(days + other, listOf(JournalEntry(date = 1, workoutId = "x", title = ""))))
    }
}
