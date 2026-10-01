package com.bugsjkeeee.tempo.content

import android.content.Context
import com.bugsjkeeee.tempo.data.TempoDao
import com.bugsjkeeee.tempo.data.UserWorkoutEntity
import com.bugsjkeeee.tempo.data.WorkoutFlagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

val AppJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Тренировки и упражнения: стартовая база из assets плюс свои тренировки и отметки из базы данных. */
class ContentRepository(private val context: Context, private val dao: TempoDao) {

    private var exercisesCache: List<Exercise>? = null
    private var baseCache: List<Workout>? = null

    suspend fun exercises(): List<Exercise> = exercisesCache ?: withContext(Dispatchers.IO) {
        context.assets.open("exercises.json").bufferedReader().use { AppJson.decodeFromString<List<Exercise>>(it.readText()) }
    }.also { exercisesCache = it }

    suspend fun exerciseMap(): Map<String, Exercise> = exercises().associateBy { it.id }

    private suspend fun baseWorkouts(): List<Workout> = baseCache ?: withContext(Dispatchers.IO) {
        context.assets.open("workouts.json").bufferedReader().use { AppJson.decodeFromString<List<Workout>>(it.readText()) }
    }.also { baseCache = it }

    val exercisesFlow: Flow<List<Exercise>> = flow { emit(exercises()) }

    /** Все тренировки: свои первыми, затем база. */
    val workouts: Flow<List<Workout>> = combine(flow { emit(baseWorkouts()) }, dao.observeUserWorkouts()) { base, user ->
        user.mapNotNull { runCatching { AppJson.decodeFromString<Workout>(it.json) }.getOrNull() }.sortedBy { it.name } + base
    }

    val flags: Flow<Map<String, WorkoutFlags>> = dao.observeFlags().map { list ->
        list.associate { it.workoutId to WorkoutFlags(it.favorite, it.hidden) }
    }

    suspend fun setFlags(workoutId: String, flags: WorkoutFlags) =
        dao.upsertFlag(WorkoutFlagEntity(workoutId, flags.favorite, flags.hidden))

    suspend fun saveUserWorkout(workout: Workout) =
        dao.upsertUserWorkout(UserWorkoutEntity(workout.id, AppJson.encodeToString(Workout.serializer(), workout)))

    suspend fun deleteUserWorkout(id: String) = dao.deleteUserWorkout(id)
}
