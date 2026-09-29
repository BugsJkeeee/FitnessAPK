package com.bugsjkeeee.fitness.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises", indices = [Index(value = ["name"], unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val isCustom: Boolean = false,
)

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startedAt: Long,
    /** null — тренировка ещё идёт. */
    val finishedAt: Long? = null,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    /** Порядок упражнения внутри тренировки. */
    val exerciseOrder: Int,
    /** Порядок подхода внутри упражнения. */
    val setOrder: Int,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val done: Boolean = false,
)

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Template::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int = 3,
)

@Entity(tableName = "body_weights")
data class BodyWeight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weight: Double,
)

/** Подход вместе с названием упражнения и временем завершения тренировки — для истории и графиков. */
data class SetWithInfo(
    val id: Long,
    val workoutId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseOrder: Int,
    val setOrder: Int,
    val weight: Double,
    val reps: Int,
    val done: Boolean,
    val finishedAt: Long?,
)

data class TemplateExerciseWithName(
    val id: Long,
    val templateId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val position: Int,
    val sets: Int,
)
