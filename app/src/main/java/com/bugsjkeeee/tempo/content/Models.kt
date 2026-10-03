package com.bugsjkeeee.tempo.content

import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.timer.TimerSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Muscle(val title: String) {
    @SerialName("chest") CHEST("Грудь"),
    @SerialName("back") BACK("Спина"),
    @SerialName("legs") LEGS("Ноги"),
    @SerialName("shoulders") SHOULDERS("Плечи"),
    @SerialName("arms") ARMS("Руки"),
    @SerialName("abs") ABS("Пресс"),
    @SerialName("full") FULL("Всё тело"),
}

@Serializable
enum class Equipment(val title: String) {
    @SerialName("bodyweight") BODYWEIGHT("Собственный вес"),
    @SerialName("barbell") BARBELL("Штанга"),
    @SerialName("dumbbell") DUMBBELL("Гантели"),
    @SerialName("kettlebell") KETTLEBELL("Гиря"),
    @SerialName("pullup_bar") PULLUP_BAR("Турник"),
    @SerialName("dip_bars") DIP_BARS("Брусья"),
    @SerialName("box") BOX("Ящик"),
    @SerialName("rower") ROWER("Гребной тренажёр"),
    @SerialName("machine") MACHINE("Тренажёры и блоки"),
    @SerialName("bench") BENCH("Скамья"),
    @SerialName("treadmill") TREADMILL("Беговая дорожка"),
    @SerialName("bike") BIKE("Велотренажёр"),
    @SerialName("elliptical") ELLIPTICAL("Эллипс"),
    @SerialName("stepper") STEPPER("Степпер"),
}

/** Кардиотренажёры: тренировка только на них считается кардио. */
val CardioEquipment = setOf(Equipment.ROWER, Equipment.TREADMILL, Equipment.BIKE, Equipment.ELLIPTICAL, Equipment.STEPPER)

@Serializable
enum class WorkoutType(val title: String) {
    @SerialName("functional") FUNCTIONAL("Функциональная"),
    @SerialName("strength") STRENGTH("Силовая"),
    @SerialName("cardio") CARDIO("Кардио"),
}

@Serializable
enum class WorkoutFormat(val title: String) {
    FOR_TIME("For Time"),
    AMRAP("AMRAP"),
    EMOM("EMOM"),
    TABATA("Табата"),
    INTERVALS("Интервалы"),
    STEADY("Непрерывно"),
    SETS("Подходы"),
}

enum class Level(val value: Int, val title: String) {
    EASY(1, "Лёгкая"), MEDIUM(2, "Средняя"), HARD(3, "Тяжёлая");

    companion object {
        fun of(value: Int) = entries.firstOrNull { it.value == value } ?: MEDIUM
    }
}

enum class DurationRange(val title: String, val range: IntRange) {
    SHORT("до 15 мин", 0..15),
    MEDIUM("15–30", 16..30),
    LONG("30–45", 31..45),
    VERY_LONG("более 45", 46..Int.MAX_VALUE),
}

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val muscles: List<Muscle>,
    val equipment: List<Equipment>,
    val images: List<String> = emptyList(),
    val technique: String = "",
)

/** Упражнение в тренировке: дозировка текстом (функциональные) или подходы × повторы (силовые). */
@Serializable
data class WorkoutItem(
    val exercise: String,
    val dose: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
)

/** Параметры таймера, с которыми запускается функциональная тренировка. */
@Serializable
data class TimerSpec(
    val mode: TimerMode,
    val capSec: Int? = null,
    val durationSec: Int? = null,
    val intervalSec: Int? = null,
    val rounds: Int? = null,
    val workSec: Int? = null,
    val restSec: Int? = null,
) {
    /** Настройки таймера на основе текущих, с параметрами тренировки поверх них. */
    fun applyTo(base: TimerSettings): TimerSettings = when (mode) {
        TimerMode.FOR_TIME -> base.copy(forTimeCapSec = capSec ?: 0)
        TimerMode.AMRAP -> base.copy(amrapSec = durationSec ?: base.amrapSec)
        TimerMode.EMOM -> base.copy(emomIntervalSec = intervalSec ?: 60, emomRounds = rounds ?: base.emomRounds)
        TimerMode.INTERVALS -> base.copy(
            workSec = workSec ?: base.workSec,
            restSec = restSec ?: base.restSec,
            intervalRounds = rounds ?: base.intervalRounds,
        )
        TimerMode.COUNTDOWN -> base.copy(countdownSec = durationSec ?: base.countdownSec)
        TimerMode.STOPWATCH -> base
    }
}

@Serializable
data class Workout(
    val id: String,
    val name: String,
    val type: WorkoutType,
    val format: WorkoutFormat,
    val level: Int,
    val durationMin: Int,
    val muscles: List<Muscle>,
    val equipment: List<Equipment>,
    val items: List<WorkoutItem>,
    val timer: TimerSpec? = null,
    val description: String = "",
    /** Тренировка добавлена пользователем. */
    val custom: Boolean = false,
    /** Программа из нескольких дней (сплит): id, название, номер дня и число дней. */
    val program: String? = null,
    val programTitle: String? = null,
    val programDay: Int? = null,
    val programDays: Int? = null,
    /** Каждый раунд таймера — следующее упражнение по кругу. */
    val rotation: Boolean = false,
)

/** Ссылка на тренировку, запущенную в таймере, — чтобы записать результат в журнал. */
data class WorkoutRef(val id: String, val name: String, val format: WorkoutFormat)
