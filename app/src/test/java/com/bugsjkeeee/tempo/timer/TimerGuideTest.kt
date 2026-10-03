package com.bugsjkeeee.tempo.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerGuideTest {
    @Test
    fun rotationPicksExerciseByRound() {
        val g = TimerGuide("Табата", listOf(GuideItem("А"), GuideItem("Б"), GuideItem("В")), rotate = true)
        assertEquals("А", g.itemForRound(1)?.name)
        assertEquals("В", g.itemForRound(3)?.name)
        assertEquals("А", g.itemForRound(4)?.name)
        assertNull(g.copy(rotate = false).itemForRound(1))
    }

    @Test
    fun rotationDoseDropsRoundMarks() {
        assertEquals("12", rotationDose("12 — нечётные минуты"))
        assertEquals("8, 43/30 кг", rotationDose("8 — нечётные, 43/30 кг"))
        assertEquals("12 кал", rotationDose("12 кал — 1-я мин"))
        assertEquals("", rotationDose("раунды 1, 5"))
        assertEquals("", rotationDose("нечётные раунды"))
        assertEquals("4 мин", rotationDose("4 мин"))
    }
}
