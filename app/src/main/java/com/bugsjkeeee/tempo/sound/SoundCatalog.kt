package com.bugsjkeeee.tempo.sound

import androidx.annotation.RawRes
import com.bugsjkeeee.tempo.R

/** События таймера, для каждого из которых выбирается свой звук. */
enum class SoundEvent(val title: String) {
    COUNTDOWN("Отсчёт 3-2-1"),
    WORK("Начало работы"),
    REST("Начало отдыха"),
    FINISH("Конец тренировки"),
}

data class Sound(val key: String, val title: String, val event: SoundEvent, @RawRes val res: Int)

object SoundCatalog {
    val all = listOf(
        Sound("count_beep", "Пик", SoundEvent.COUNTDOWN, R.raw.count_beep),
        Sound("count_beep_high", "Высокий пик", SoundEvent.COUNTDOWN, R.raw.count_beep_high),
        Sound("count_click", "Щелчок", SoundEvent.COUNTDOWN, R.raw.count_click),
        Sound("count_wood", "Деревянный блок", SoundEvent.COUNTDOWN, R.raw.count_wood),
        Sound("count_digital", "Электронный двойной", SoundEvent.COUNTDOWN, R.raw.count_digital),

        Sound("work_boxing_gong", "Боксёрский гонг", SoundEvent.WORK, R.raw.work_boxing_gong),
        Sound("work_air_horn", "Воздушный гудок", SoundEvent.WORK, R.raw.work_air_horn),
        Sound("work_whistle", "Свисток", SoundEvent.WORK, R.raw.work_whistle),
        Sound("work_race_start", "Старт гонки", SoundEvent.WORK, R.raw.work_race_start),
        Sound("work_bell", "Звонок", SoundEvent.WORK, R.raw.work_bell),
        Sound("work_buzzer", "Баскетбольная сирена", SoundEvent.WORK, R.raw.work_buzzer),

        Sound("rest_chime", "Колокольчик", SoundEvent.REST, R.raw.rest_chime),
        Sound("rest_ding_dong", "Динь-дон", SoundEvent.REST, R.raw.rest_ding_dong),
        Sound("rest_soft_beep", "Мягкий сигнал", SoundEvent.REST, R.raw.rest_soft_beep),
        Sound("rest_down", "Нисходящий тон", SoundEvent.REST, R.raw.rest_down),

        Sound("finish_triple_gong", "Тройной гонг", SoundEvent.FINISH, R.raw.finish_triple_gong),
        Sound("finish_fanfare", "Фанфары", SoundEvent.FINISH, R.raw.finish_fanfare),
        Sound("finish_long_horn", "Длинный гудок", SoundEvent.FINISH, R.raw.finish_long_horn),
        Sound("finish_victory", "Победная мелодия", SoundEvent.FINISH, R.raw.finish_victory),
        Sound("finish_bells", "Колокола", SoundEvent.FINISH, R.raw.finish_bells),
    )

    val defaults = mapOf(
        SoundEvent.COUNTDOWN to "count_beep",
        SoundEvent.WORK to "work_boxing_gong",
        SoundEvent.REST to "rest_chime",
        SoundEvent.FINISH to "finish_triple_gong",
    )

    fun forEvent(event: SoundEvent) = all.filter { it.event == event }

    /** Звук по ключу, но только из группы события — иначе звук по умолчанию. */
    fun resolve(event: SoundEvent, key: String?): Sound =
        all.firstOrNull { it.key == key && it.event == event } ?: all.first { it.key == defaults.getValue(event) }
}
