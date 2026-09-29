package com.bugsjkeeee.fitness.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {

    // --- Упражнения ---

    @Query("SELECT * FROM exercises ORDER BY muscleGroup, name")
    fun observeExercises(): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Query(
        """
        SELECT DISTINCT e.* FROM exercises e
        JOIN workout_sets s ON s.exerciseId = e.id
        JOIN workouts w ON w.id = s.workoutId
        WHERE w.finishedAt IS NOT NULL AND s.done = 1
        ORDER BY e.name
        """
    )
    fun observeExercisesWithHistory(): Flow<List<Exercise>>

    // --- Тренировки ---

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkout(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeWorkout(): Workout?

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWorkout(id: Long): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun observeFinishedWorkouts(): Flow<List<Workout>>

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    // --- Подходы ---

    @Query(
        """
        SELECT s.id, s.workoutId, s.exerciseId, e.name AS exerciseName, s.exerciseOrder, s.setOrder,
               s.weight, s.reps, s.done, w.finishedAt
        FROM workout_sets s
        JOIN exercises e ON e.id = s.exerciseId
        JOIN workouts w ON w.id = s.workoutId
        WHERE s.workoutId = :workoutId
        ORDER BY s.exerciseOrder, s.setOrder
        """
    )
    fun observeSetsForWorkout(workoutId: Long): Flow<List<SetWithInfo>>

    @Query(
        """
        SELECT s.id, s.workoutId, s.exerciseId, e.name AS exerciseName, s.exerciseOrder, s.setOrder,
               s.weight, s.reps, s.done, w.finishedAt
        FROM workout_sets s
        JOIN exercises e ON e.id = s.exerciseId
        JOIN workouts w ON w.id = s.workoutId
        WHERE w.finishedAt IS NOT NULL AND s.done = 1
        ORDER BY w.finishedAt, s.exerciseOrder, s.setOrder
        """
    )
    fun observeAllFinishedSets(): Flow<List<SetWithInfo>>

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY exerciseOrder, setOrder")
    suspend fun setsForWorkout(workoutId: Long): List<WorkoutSet>

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun set(id: Long): WorkoutSet?

    @Insert
    suspend fun insertSet(set: WorkoutSet): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Update
    suspend fun updateSet(set: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND exerciseOrder = :exerciseOrder")
    suspend fun deleteExerciseFromWorkout(workoutId: Long, exerciseOrder: Int)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND done = 0")
    suspend fun deleteUndoneSets(workoutId: Long)

    // --- Шаблоны ---

    @Query("SELECT * FROM templates ORDER BY name")
    fun observeTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun template(id: Long): Template?

    @Query(
        """
        SELECT t.id, t.templateId, t.exerciseId, e.name AS exerciseName, t.position, t.sets
        FROM template_exercises t JOIN exercises e ON e.id = t.exerciseId
        WHERE t.templateId = :templateId ORDER BY t.position
        """
    )
    suspend fun templateExercises(templateId: Long): List<TemplateExerciseWithName>

    @Query(
        """
        SELECT t.id, t.templateId, t.exerciseId, e.name AS exerciseName, t.position, t.sets
        FROM template_exercises t JOIN exercises e ON e.id = t.exerciseId
        ORDER BY t.templateId, t.position
        """
    )
    fun observeAllTemplateExercises(): Flow<List<TemplateExerciseWithName>>

    @Insert
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun clearTemplateExercises(templateId: Long)

    @Insert
    suspend fun insertTemplateExercises(items: List<TemplateExercise>)

    // --- Вес тела ---

    @Query("SELECT * FROM body_weights ORDER BY date DESC")
    fun observeBodyWeights(): Flow<List<BodyWeight>>

    @Insert
    suspend fun insertBodyWeight(entry: BodyWeight)

    @Delete
    suspend fun deleteBodyWeight(entry: BodyWeight)
}
