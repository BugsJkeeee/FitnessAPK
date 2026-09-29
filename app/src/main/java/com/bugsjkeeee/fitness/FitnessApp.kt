package com.bugsjkeeee.fitness

import android.app.Application
import com.bugsjkeeee.fitness.data.FitnessDatabase
import com.bugsjkeeee.fitness.data.FitnessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FitnessApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: FitnessRepository by lazy { FitnessRepository(FitnessDatabase.create(this)) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch { repository.seedIfEmpty() }
    }
}
