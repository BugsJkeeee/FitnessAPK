package com.bugsjkeeee.tempo

import android.app.Application
import com.bugsjkeeee.tempo.content.ContentRepository
import com.bugsjkeeee.tempo.data.BackupManager
import com.bugsjkeeee.tempo.data.JournalRepository
import com.bugsjkeeee.tempo.data.TempoDatabase
import com.bugsjkeeee.tempo.data.WeightRepository
import com.bugsjkeeee.tempo.settings.SettingsRepository
import com.bugsjkeeee.tempo.sound.AudioPlayer
import com.bugsjkeeee.tempo.timer.TimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Контейнер зависимостей приложения. */
class TempoApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { TempoDatabase.create(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val contentRepository by lazy { ContentRepository(this, database.dao()) }
    val journalRepository by lazy { JournalRepository(database) }
    val weightRepository by lazy { WeightRepository(database.dao()) }
    val backupManager by lazy { BackupManager(this, database, settingsRepository) }
    val audioPlayer by lazy { AudioPlayer(this).also { it.prepareVoice() } }
    val timerController by lazy { TimerController(this, settingsRepository, audioPlayer) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch { runCatching { backupManager.autoBackupIfDue() } }
    }
}
