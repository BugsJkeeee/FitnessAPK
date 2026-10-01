package com.bugsjkeeee.tempo.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Своя тренировка пользователя хранится целиком в JSON — её структура совпадает с тренировками базы. */
@Entity(tableName = "user_workouts")
data class UserWorkoutEntity(@PrimaryKey val id: String, val json: String)

@Entity(tableName = "workout_flags")
data class WorkoutFlagEntity(@PrimaryKey val workoutId: String, val favorite: Boolean, val hidden: Boolean)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val workoutId: String?,
    val title: String,
    val format: String?,
    val timerMode: String?,
    val durationMs: Long?,
    val resultTimeMs: Long?,
    val resultRounds: Int?,
    val note: String,
)

@Entity(
    tableName = "journal_sets",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId"), Index("exerciseId")],
)
data class JournalSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val position: Int,
    val exerciseId: String,
    val setIndex: Int,
    val weight: Double?,
    val reps: Int?,
)

data class EntryWithSets(
    @Embedded val entry: JournalEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId") val sets: List<JournalSetEntity>,
)

@Entity(tableName = "body_weights")
data class BodyWeightEntity(@PrimaryKey val epochDay: Long, val weight: Double)

@Dao
interface TempoDao {
    @Query("SELECT * FROM user_workouts")
    fun observeUserWorkouts(): Flow<List<UserWorkoutEntity>>

    @Query("SELECT * FROM user_workouts")
    suspend fun userWorkouts(): List<UserWorkoutEntity>

    @Upsert
    suspend fun upsertUserWorkout(item: UserWorkoutEntity)

    @Query("DELETE FROM user_workouts WHERE id = :id")
    suspend fun deleteUserWorkout(id: String)

    @Query("SELECT * FROM workout_flags")
    fun observeFlags(): Flow<List<WorkoutFlagEntity>>

    @Query("SELECT * FROM workout_flags")
    suspend fun flags(): List<WorkoutFlagEntity>

    @Upsert
    suspend fun upsertFlag(flag: WorkoutFlagEntity)

    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeJournal(): Flow<List<EntryWithSets>>

    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    suspend fun journal(): List<EntryWithSets>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun entry(id: Long): EntryWithSets?

    @Insert
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceEntry(entry: JournalEntryEntity): Long

    @Insert
    suspend fun insertSets(sets: List<JournalSetEntity>)

    @Query("DELETE FROM journal_sets WHERE entryId = :entryId")
    suspend fun deleteSets(entryId: Long)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT * FROM body_weights ORDER BY epochDay DESC")
    fun observeWeights(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weights")
    suspend fun weights(): List<BodyWeightEntity>

    @Upsert
    suspend fun upsertWeight(item: BodyWeightEntity)

    @Query("DELETE FROM body_weights WHERE epochDay = :epochDay")
    suspend fun deleteWeight(epochDay: Long)

    @Query("DELETE FROM user_workouts")
    suspend fun clearUserWorkouts()

    @Query("DELETE FROM workout_flags")
    suspend fun clearFlags()

    @Query("DELETE FROM journal_entries")
    suspend fun clearJournal()

    @Query("DELETE FROM body_weights")
    suspend fun clearWeights()
}

@Database(
    entities = [
        UserWorkoutEntity::class,
        WorkoutFlagEntity::class,
        JournalEntryEntity::class,
        JournalSetEntity::class,
        BodyWeightEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TempoDatabase : RoomDatabase() {
    abstract fun dao(): TempoDao

    companion object {
        fun create(context: Context): TempoDatabase =
            Room.databaseBuilder(context, TempoDatabase::class.java, "tempo.db").build()
    }
}
