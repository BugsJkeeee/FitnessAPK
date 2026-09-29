package com.bugsjkeeee.fitness.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Exercise::class,
        Workout::class,
        WorkoutSet::class,
        Template::class,
        TemplateExercise::class,
        BodyWeight::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun dao(): FitnessDao

    companion object {
        fun create(context: Context): FitnessDatabase =
            Room.databaseBuilder(context, FitnessDatabase::class.java, "fitness.db").build()
    }
}
