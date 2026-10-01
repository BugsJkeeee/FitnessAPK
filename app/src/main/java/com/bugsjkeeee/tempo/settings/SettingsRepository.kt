package com.bugsjkeeee.tempo.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bugsjkeeee.tempo.sound.SoundCatalog
import com.bugsjkeeee.tempo.sound.SoundEvent
import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.timer.TimerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class SoundMode(val title: String) { SIGNALS("Сигналы"), VOICE("Голос") }

enum class ThemeMode(val title: String) { SYSTEM("Как в телефоне"), DARK("Тёмная"), LIGHT("Светлая") }

data class AppSettings(
    val soundMode: SoundMode = SoundMode.SIGNALS,
    val sounds: Map<SoundEvent, String> = SoundCatalog.defaults,
    val prepSec: Int = 10,
    val targetWeight: Double? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val soundMode = stringPreferencesKey("sound_mode")
        val prepSec = intPreferencesKey("prep_sec")
        val targetWeight = doublePreferencesKey("target_weight")
        val themeMode = stringPreferencesKey("theme_mode")
        val lastAutoBackup = longPreferencesKey("last_auto_backup")
        fun sound(event: SoundEvent) = stringPreferencesKey("sound_${event.name.lowercase()}")

        val lastMode = stringPreferencesKey("timer_last_mode")
        val countdownSec = intPreferencesKey("timer_countdown_sec")
        val forTimeCapSec = intPreferencesKey("timer_for_time_cap_sec")
        val amrapSec = intPreferencesKey("timer_amrap_sec")
        val emomIntervalSec = intPreferencesKey("timer_emom_interval_sec")
        val emomRounds = intPreferencesKey("timer_emom_rounds")
        val workSec = intPreferencesKey("timer_work_sec")
        val restSec = intPreferencesKey("timer_rest_sec")
        val intervalRounds = intPreferencesKey("timer_interval_rounds")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            soundMode = p[Keys.soundMode]?.let { runCatching { SoundMode.valueOf(it) }.getOrNull() } ?: SoundMode.SIGNALS,
            sounds = SoundEvent.entries.associateWith { SoundCatalog.resolve(it, p[Keys.sound(it)]).key },
            prepSec = p[Keys.prepSec] ?: 10,
            targetWeight = p[Keys.targetWeight],
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        )
    }

    val timerSettings: Flow<TimerSettings> = context.dataStore.data.map { p ->
        val d = TimerSettings()
        TimerSettings(
            countdownSec = p[Keys.countdownSec] ?: d.countdownSec,
            forTimeCapSec = p[Keys.forTimeCapSec] ?: d.forTimeCapSec,
            amrapSec = p[Keys.amrapSec] ?: d.amrapSec,
            emomIntervalSec = p[Keys.emomIntervalSec] ?: d.emomIntervalSec,
            emomRounds = p[Keys.emomRounds] ?: d.emomRounds,
            workSec = p[Keys.workSec] ?: d.workSec,
            restSec = p[Keys.restSec] ?: d.restSec,
            intervalRounds = p[Keys.intervalRounds] ?: d.intervalRounds,
        )
    }

    val lastMode: Flow<TimerMode> = context.dataStore.data.map { p ->
        p[Keys.lastMode]?.let { runCatching { TimerMode.valueOf(it) }.getOrNull() } ?: TimerMode.INTERVALS
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setSoundMode(mode: SoundMode) = context.dataStore.edit { it[Keys.soundMode] = mode.name }
    suspend fun setSound(event: SoundEvent, key: String) = context.dataStore.edit { it[Keys.sound(event)] = key }
    suspend fun setPrepSec(sec: Int) = context.dataStore.edit { it[Keys.prepSec] = sec }
    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.themeMode] = mode.name }

    suspend fun setTargetWeight(weight: Double?) = context.dataStore.edit {
        if (weight == null) it.remove(Keys.targetWeight) else it[Keys.targetWeight] = weight
    }

    suspend fun lastAutoBackup(): Long = context.dataStore.data.first()[Keys.lastAutoBackup] ?: 0L
    suspend fun setLastAutoBackup(time: Long) = context.dataStore.edit { it[Keys.lastAutoBackup] = time }

    /** Восстановление настроек из резервной копии. */
    suspend fun restore(settings: AppSettings) = context.dataStore.edit { p ->
        p[Keys.soundMode] = settings.soundMode.name
        p[Keys.prepSec] = settings.prepSec
        settings.sounds.forEach { (event, key) -> p[Keys.sound(event)] = key }
        if (settings.targetWeight == null) p.remove(Keys.targetWeight) else p[Keys.targetWeight] = settings.targetWeight
    }

    suspend fun setLastMode(mode: TimerMode) = context.dataStore.edit { it[Keys.lastMode] = mode.name }

    suspend fun saveTimerSettings(s: TimerSettings) = context.dataStore.edit { p ->
        p[Keys.countdownSec] = s.countdownSec
        p[Keys.forTimeCapSec] = s.forTimeCapSec
        p[Keys.amrapSec] = s.amrapSec
        p[Keys.emomIntervalSec] = s.emomIntervalSec
        p[Keys.emomRounds] = s.emomRounds
        p[Keys.workSec] = s.workSec
        p[Keys.restSec] = s.restSec
        p[Keys.intervalRounds] = s.intervalRounds
    }
}
