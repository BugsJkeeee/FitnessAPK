package com.bugsjkeeee.tempo.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.bugsjkeeee.tempo.content.AppJson
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.settings.AppSettings
import com.bugsjkeeee.tempo.settings.SettingsRepository
import com.bugsjkeeee.tempo.settings.SoundMode
import com.bugsjkeeee.tempo.sound.SoundEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.time.LocalDate

@Serializable
data class BackupFlag(val workoutId: String, val favorite: Boolean, val hidden: Boolean)

@Serializable
data class BackupWeight(val epochDay: Long, val weight: Double)

@Serializable
data class BackupSettings(
    val soundMode: String,
    val sounds: Map<String, String>,
    val prepSec: Int,
    val targetWeight: Double? = null,
)

/** Содержимое файла резервной копии: все данные пользователя и настройки. */
@Serializable
data class BackupData(
    val version: Int = 1,
    val createdAt: Long,
    val userWorkouts: List<Workout>,
    val flags: List<BackupFlag>,
    val journal: List<JournalEntry>,
    val weights: List<BackupWeight>,
    val settings: BackupSettings,
)

class BackupManager(
    private val context: Context,
    private val db: TempoDatabase,
    private val settings: SettingsRepository,
) {
    private val dao = db.dao()

    suspend fun export(): String {
        val s = settings.current()
        val data = BackupData(
            createdAt = System.currentTimeMillis(),
            userWorkouts = dao.userWorkouts().mapNotNull { runCatching { AppJson.decodeFromString<Workout>(it.json) }.getOrNull() },
            flags = dao.flags().map { BackupFlag(it.workoutId, it.favorite, it.hidden) },
            journal = dao.journal().map { it.toDomain() },
            weights = dao.weights().map { BackupWeight(it.epochDay, it.weight) },
            settings = BackupSettings(s.soundMode.name, s.sounds.mapKeys { it.key.name }, s.prepSec, s.targetWeight),
        )
        return AppJson.encodeToString(BackupData.serializer(), data)
    }

    /** Полностью заменяет данные содержимым копии. Бросает исключение, если файл не является копией Tempo. */
    suspend fun restore(json: String) {
        val data = AppJson.decodeFromString(BackupData.serializer(), json)
        db.withTransaction {
            dao.clearUserWorkouts()
            dao.clearFlags()
            dao.clearJournal()
            dao.clearWeights()
            data.userWorkouts.forEach { dao.upsertUserWorkout(UserWorkoutEntity(it.id, AppJson.encodeToString(Workout.serializer(), it))) }
            data.flags.forEach { dao.upsertFlag(WorkoutFlagEntity(it.workoutId, it.favorite, it.hidden)) }
            data.journal.forEach { entry ->
                val id = dao.insertEntry(entry.copy(id = 0).toEntity())
                dao.insertSets(entry.setEntities(id))
            }
            data.weights.forEach { dao.upsertWeight(BodyWeightEntity(it.epochDay, it.weight)) }
        }
        settings.restore(
            AppSettings(
                soundMode = runCatching { SoundMode.valueOf(data.settings.soundMode) }.getOrDefault(SoundMode.SIGNALS),
                sounds = data.settings.sounds.mapNotNull { (k, v) -> runCatching { SoundEvent.valueOf(k) }.getOrNull()?.let { it to v } }.toMap(),
                prepSec = data.settings.prepSec,
                targetWeight = data.settings.targetWeight,
            ),
        )
    }

    private fun fileName() = "tempo-backup-${LocalDate.now()}.json"

    /** Файл копии во временной папке и намерение «Поделиться» для отправки на Диск, в Telegram и т.п. */
    suspend fun shareIntent(): Intent {
        val text = export()
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, "backup").apply { mkdirs() }.resolve(fileName()).apply { writeText(text) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return Intent(Intent.ACTION_SEND)
            .setType("application/json")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /**
     * Раз в неделю сохраняет копию в «Загрузки/Tempo». Проверка выполняется при запуске приложения.
     * На Android 9 и ниже запись в общую папку требует отдельного разрешения, поэтому там копия делается только вручную.
     */
    suspend fun autoBackupIfDue() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val now = System.currentTimeMillis()
        if (now - settings.lastAutoBackup() < 7L * 24 * 60 * 60 * 1000) return
        if (dao.journal().isEmpty() && dao.weights().isEmpty() && dao.userWorkouts().isEmpty()) return
        val text = export()
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName())
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Tempo")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext
            resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        }
        settings.setLastAutoBackup(now)
    }
}
