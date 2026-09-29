package com.bugsjkeeee.fitness.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class FitnessRepository(private val db: FitnessDatabase) {
    private val dao = db.dao()

    suspend fun seedIfEmpty() {
        if (dao.exerciseCount() == 0) dao.insertExercises(DefaultExercises.all)
    }

    // --- Упражнения ---

    val exercises: Flow<List<Exercise>> = dao.observeExercises()
    val exercisesWithHistory: Flow<List<Exercise>> = dao.observeExercisesWithHistory()

    /** Возвращает id нового упражнения или null, если упражнение с таким названием уже есть. */
    suspend fun addCustomExercise(name: String, muscleGroup: String): Long? {
        val id = dao.insertExercise(Exercise(name = name.trim(), muscleGroup = muscleGroup, isCustom = true))
        return id.takeIf { it > 0 }
    }

    // --- Тренировки ---

    val activeWorkout: Flow<Workout?> = dao.observeActiveWorkout()
    val finishedWorkouts: Flow<List<Workout>> = dao.observeFinishedWorkouts()
    val allFinishedSets: Flow<List<SetWithInfo>> = dao.observeAllFinishedSets()

    fun workout(id: Long): Flow<Workout?> = dao.observeWorkout(id)
    fun setsForWorkout(id: Long): Flow<List<SetWithInfo>> = dao.observeSetsForWorkout(id)

    /** Начинает тренировку; если уже есть незавершённая — возвращает её. */
    suspend fun startWorkout(templateId: Long? = null): Long = db.withTransaction {
        dao.activeWorkout()?.let { return@withTransaction it.id }
        val template = templateId?.let { dao.template(it) }
        val workoutId = dao.insertWorkout(
            Workout(name = template?.name ?: "Тренировка", startedAt = System.currentTimeMillis())
        )
        if (template != null) {
            val sets = dao.templateExercises(template.id).flatMapIndexed { order, item ->
                List(item.sets.coerceAtLeast(1)) { setOrder ->
                    WorkoutSet(
                        workoutId = workoutId,
                        exerciseId = item.exerciseId,
                        exerciseOrder = order,
                        setOrder = setOrder,
                    )
                }
            }
            dao.insertSets(sets)
        }
        workoutId
    }

    suspend fun renameWorkout(workout: Workout, name: String) =
        dao.updateWorkout(workout.copy(name = name))

    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long) {
        val order = (dao.setsForWorkout(workoutId).maxOfOrNull { it.exerciseOrder } ?: -1) + 1
        dao.insertSet(WorkoutSet(workoutId = workoutId, exerciseId = exerciseId, exerciseOrder = order, setOrder = 0))
    }

    /** Добавляет подход, копируя вес и повторения из последнего подхода упражнения. */
    suspend fun addSet(workoutId: Long, exerciseOrder: Int) {
        val sets = dao.setsForWorkout(workoutId).filter { it.exerciseOrder == exerciseOrder }
        val last = sets.maxByOrNull { it.setOrder } ?: return
        dao.insertSet(last.copy(id = 0, setOrder = last.setOrder + 1, done = false))
    }

    suspend fun updateSet(id: Long, weight: Double, reps: Int, done: Boolean) {
        val set = dao.set(id) ?: return
        dao.updateSet(set.copy(weight = weight, reps = reps, done = done))
    }

    suspend fun deleteSet(id: Long) = dao.deleteSet(id)

    suspend fun removeExerciseFromWorkout(workoutId: Long, exerciseOrder: Int) =
        dao.deleteExerciseFromWorkout(workoutId, exerciseOrder)

    /** Завершает тренировку; невыполненные подходы удаляются, пустая тренировка удаляется целиком. */
    suspend fun finishWorkout(workout: Workout): Boolean = db.withTransaction {
        dao.deleteUndoneSets(workout.id)
        if (dao.setsForWorkout(workout.id).isEmpty()) {
            dao.deleteWorkout(workout.id)
            false
        } else {
            dao.updateWorkout(workout.copy(finishedAt = System.currentTimeMillis()))
            true
        }
    }

    suspend fun deleteWorkout(id: Long) = dao.deleteWorkout(id)

    // --- Шаблоны ---

    val templates: Flow<List<Template>> = dao.observeTemplates()
    val allTemplateExercises: Flow<List<TemplateExerciseWithName>> = dao.observeAllTemplateExercises()

    suspend fun template(id: Long): Template? = dao.template(id)
    suspend fun templateExercises(id: Long): List<TemplateExerciseWithName> = dao.templateExercises(id)

    suspend fun saveTemplate(template: Template, items: List<TemplateExercise>): Long = db.withTransaction {
        val id = if (template.id == 0L) {
            dao.insertTemplate(template)
        } else {
            dao.updateTemplate(template)
            template.id
        }
        dao.clearTemplateExercises(id)
        dao.insertTemplateExercises(items.mapIndexed { i, item -> item.copy(id = 0, templateId = id, position = i) })
        id
    }

    suspend fun deleteTemplate(id: Long) = dao.deleteTemplate(id)

    // --- Вес тела ---

    val bodyWeights: Flow<List<BodyWeight>> = dao.observeBodyWeights()

    suspend fun addBodyWeight(weight: Double) =
        dao.insertBodyWeight(BodyWeight(date = System.currentTimeMillis(), weight = weight))

    suspend fun deleteBodyWeight(entry: BodyWeight) = dao.deleteBodyWeight(entry)
}
