package com.bugsjkeeee.tempo

import android.app.Application
import com.bugsjkeeee.tempo.settings.SettingsRepository
import com.bugsjkeeee.tempo.sound.AudioPlayer
import com.bugsjkeeee.tempo.timer.TimerController

/** Контейнер зависимостей приложения. */
class TempoApp : Application() {
    val settingsRepository by lazy { SettingsRepository(this) }
    val audioPlayer by lazy { AudioPlayer(this).also { it.prepareVoice() } }
    val timerController by lazy { TimerController(this, settingsRepository, audioPlayer) }
}
