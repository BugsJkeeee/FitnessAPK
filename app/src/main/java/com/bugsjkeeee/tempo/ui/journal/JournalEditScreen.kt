package com.bugsjkeeee.tempo.ui.journal

import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.content.Exercise
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.content.WorkoutFormat
import com.bugsjkeeee.tempo.data.JournalEntry
import com.bugsjkeeee.tempo.data.JournalSet
import com.bugsjkeeee.tempo.data.isNewRecord
import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.base.BackTopBar
import com.bugsjkeeee.tempo.ui.components.CountStepper
import com.bugsjkeeee.tempo.ui.components.DurationStepper
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.formatDate
import com.bugsjkeeee.tempo.ui.components.formatWeight
import com.bugsjkeeee.tempo.ui.components.parseDecimal
import com.bugsjkeeee.tempo.ui.execute.SetRow
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

data class EditBlock(val exerciseId: String, val sets: List<SetRow>)

data class JournalDraft(
    val loaded: Boolean = false,
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val workoutId: String? = null,
    val title: String = "",
    val format: WorkoutFormat? = null,
    val timerMode: TimerMode? = null,
    val durationSec: Int = 0,
    val resultTimeSec: Int = 0,
    val resultRounds: Int = 0,
    val note: String = "",
    val blocks: List<EditBlock> = emptyList(),
    val exercises: Map<String, Exercise> = emptyMap(),
    val fromTimer: Boolean = false,
)

private fun TimerMode.toFormat(): WorkoutFormat? = when (this) {
    TimerMode.FOR_TIME -> WorkoutFormat.FOR_TIME
    TimerMode.AMRAP -> WorkoutFormat.AMRAP
    TimerMode.EMOM -> WorkoutFormat.EMOM
    TimerMode.INTERVALS -> WorkoutFormat.TABATA
    else -> null
}

/** Подходы по упражнениям тренировки: у силовых — по числу подходов с целевыми повторами, у комплексов — один. */
private fun Workout.blocks(): List<EditBlock> = items.map { item ->
    EditBlock(item.exercise, List(item.sets ?: 1) { SetRow(reps = item.reps?.toString().orEmpty()) })
}

class JournalEditViewModel(
    private val app: TempoApp,
    private val entryId: Long,
    private val workoutId: String?,
    private val fromTimer: Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(JournalDraft())
    val state: StateFlow<JournalDraft> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val exercises = app.contentRepository.exerciseMap()
            val workouts = app.contentRepository.workouts.first()
            val existing = if (entryId != 0L) app.journalRepository.get(entryId) else null
            _state.value = when {
                existing != null -> JournalDraft(
                    loaded = true,
                    id = existing.id,
                    date = existing.date,
                    workoutId = existing.workoutId,
                    title = existing.title,
                    format = existing.format,
                    timerMode = existing.timerMode,
                    durationSec = ((existing.durationMs ?: 0) / 1000).toInt(),
                    resultTimeSec = ((existing.resultTimeMs ?: 0) / 1000).toInt(),
                    resultRounds = existing.resultRounds ?: 0,
                    note = existing.note,
                    blocks = existing.sets.groupBy { it.exerciseId }.map { (ex, sets) ->
                        EditBlock(ex, sets.map { SetRow(it.weight?.let(::formatWeight).orEmpty(), it.reps?.toString().orEmpty()) })
                    },
                    exercises = exercises,
                )
                fromTimer -> {
                    val snap = app.timerController.state.value
                    val w = snap?.workout?.let { ref -> workouts.firstOrNull { it.id == ref.id } }
                    JournalDraft(
                        loaded = true,
                        date = System.currentTimeMillis() - (snap?.totalElapsedMs ?: 0),
                        workoutId = w?.id,
                        title = w?.name ?: snap?.mode?.title.orEmpty(),
                        format = w?.format ?: snap?.mode?.toFormat(),
                        timerMode = snap?.mode,
                        durationSec = ((snap?.totalElapsedMs ?: 0) / 1000).toInt(),
                        resultTimeSec = if (snap?.mode == TimerMode.FOR_TIME) ((snap?.totalElapsedMs ?: 0) / 1000).toInt() else 0,
                        resultRounds = if (snap?.mode == TimerMode.AMRAP) snap?.laps?.size ?: 0 else 0,
                        blocks = w?.blocks().orEmpty(),
                        exercises = exercises,
                        fromTimer = true,
                    )
                }
                else -> JournalDraft(loaded = true, exercises = exercises).let { d ->
                    workoutId?.let { id -> workouts.firstOrNull { it.id == id } }?.let { applyWorkout(d, it) } ?: d
                }
            }
        }
    }

    private fun applyWorkout(d: JournalDraft, w: Workout) = d.copy(
        workoutId = w.id,
        title = w.name,
        format = w.format,
        durationSec = w.durationMin * 60,
        blocks = w.blocks(),
    )

    fun pickWorkout(id: String) = viewModelScope.launch {
        val w = app.contentRepository.workouts.first().firstOrNull { it.id == id } ?: return@launch
        _state.update { applyWorkout(it, w) }
    }

    fun update(transform: (JournalDraft) -> JournalDraft) = _state.update(transform)

    fun addExercise(exerciseId: String) = _state.update { it.copy(blocks = it.blocks + EditBlock(exerciseId, listOf(SetRow()))) }

    fun updateSet(b: Int, s: Int, transform: (SetRow) -> SetRow) = _state.update { d ->
        d.copy(blocks = d.blocks.mapIndexed { bi, block ->
            if (bi != b) block else block.copy(sets = block.sets.mapIndexed { si, row -> if (si == s) transform(row) else row })
        })
    }

    fun addSet(b: Int) = _state.update { d ->
        d.copy(blocks = d.blocks.mapIndexed { bi, block ->
            if (bi != b) block else block.copy(sets = block.sets + SetRow(weight = block.sets.lastOrNull()?.weight.orEmpty()))
        })
    }

    fun removeSet(b: Int, s: Int) = _state.update { d ->
        d.copy(blocks = d.blocks.mapIndexedNotNull { bi, block ->
            if (bi != b) block else block.copy(sets = block.sets.filterIndexed { si, _ -> si != s }).takeIf { it.sets.isNotEmpty() }
        })
    }

    /** Сохраняет запись; возвращает, побит ли рекорд. */
    suspend fun save(): Boolean {
        val d = _state.value
        val entry = JournalEntry(
            id = d.id,
            date = d.date,
            workoutId = d.workoutId,
            title = d.title.trim().ifEmpty { "Тренировка" },
            format = d.format,
            timerMode = d.timerMode,
            durationMs = d.durationSec.takeIf { it > 0 }?.times(1000L),
            resultTimeMs = if (d.format == WorkoutFormat.FOR_TIME && d.resultTimeSec > 0) d.resultTimeSec * 1000L else null,
            resultRounds = if (d.format == WorkoutFormat.AMRAP) d.resultRounds else null,
            note = d.note.trim(),
            sets = d.blocks.flatMap { block ->
                block.sets.filter { it.weight.isNotBlank() || it.reps.isNotBlank() }.mapIndexed { i, row ->
                    JournalSet(block.exerciseId, i + 1, parseDecimal(row.weight), row.reps.toIntOrNull())
                }
            },
        )
        val previous = app.journalRepository.all()
        app.journalRepository.save(entry)
        if (d.fromTimer) app.timerController.dismiss()
        return isNewRecord(entry, previous)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditScreen(
    entryId: Long,
    workoutId: String?,
    fromTimer: Boolean,
    pickedExercise: String?,
    pickedWorkout: String?,
    onPickedConsumed: () -> Unit,
    onPickExercise: () -> Unit,
    onPickWorkout: () -> Unit,
    onDone: () -> Unit,
) {
    val vm = appViewModel { JournalEditViewModel(it, entryId, workoutId, fromTimer) }
    val d by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val style = LocalTempoStyle.current
    var showDate by rememberSaveable { mutableStateOf(false) }
    var record by rememberSaveable { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(pickedExercise, pickedWorkout, d.loaded) {
        if (!d.loaded) return@LaunchedEffect
        pickedExercise?.let { vm.addExercise(it) }
        pickedWorkout?.let { vm.pickWorkout(it) }
        if (pickedExercise != null || pickedWorkout != null) onPickedConsumed()
    }

    Scaffold(topBar = { BackTopBar(if (entryId == 0L) "Запись в журнал" else "Редактирование", onDone) }) { padding ->
        if (!d.loaded) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (entryId == 0L && !d.fromTimer && d.workoutId == null) {
                item {
                    OutlinedButton(onClick = onPickWorkout, modifier = Modifier.fillMaxWidth()) {
                        Icon(TempoIcons.Dumbbell, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Выбрать тренировку из базы")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = d.title,
                    onValueChange = { v -> vm.update { it.copy(title = v) } },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(TempoIcons.Calendar, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Дата: " + formatDate(d.date))
                }
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Результат")
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DurationStepper("Длительность", d.durationSec, { v -> vm.update { it.copy(durationSec = v) } }, min = 0, zeroText = "—")
                        when (d.format) {
                            WorkoutFormat.FOR_TIME -> DurationStepper("Время комплекса", d.resultTimeSec, { v -> vm.update { it.copy(resultTimeSec = v) } }, min = 0, zeroText = "—")
                            WorkoutFormat.AMRAP -> CountStepper("Раунды", d.resultRounds, { v -> vm.update { it.copy(resultRounds = v) } }, min = 0, max = 999)
                            else -> Unit
                        }
                    }
                }
            }
            if (d.blocks.isNotEmpty()) item { TileLabel("Упражнения и веса") }
            itemsIndexed(d.blocks) { b, block ->
                Tile(Modifier.fillMaxWidth()) {
                    Text(d.exercises[block.exerciseId]?.name ?: block.exerciseId, style = MaterialTheme.typography.titleMedium)
                    block.sets.forEachIndexed { s, row ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${s + 1}", modifier = Modifier.width(20.dp))
                            OutlinedTextField(
                                value = row.weight,
                                onValueChange = { v -> vm.updateSet(b, s) { it.copy(weight = v) } },
                                label = { Text("кг") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = row.reps,
                                onValueChange = { v -> vm.updateSet(b, s) { it.copy(reps = v.filter(Char::isDigit).take(3)) } },
                                label = { Text("повт.") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { vm.removeSet(b, s) }) { Icon(TempoIcons.Close, contentDescription = "Удалить подход") }
                        }
                    }
                    TextButton(onClick = { vm.addSet(b) }) {
                        Icon(TempoIcons.Add, contentDescription = null)
                        Text("Подход")
                    }
                }
            }
            item {
                OutlinedButton(onClick = onPickExercise, modifier = Modifier.fillMaxWidth()) {
                    Icon(TempoIcons.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Упражнение")
                }
            }
            item {
                OutlinedTextField(
                    value = d.note,
                    onValueChange = { v -> vm.update { it.copy(note = v) } },
                    label = { Text("Заметка") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    onClick = { scope.launch { record = vm.save() } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = style.tileShape,
                ) { Text("Сохранить", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }

    if (showDate) {
        val zone = ZoneId.systemDefault()
        val local = Instant.ofEpochMilli(d.date).atZone(zone)
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    showDate = false
                    pickerState.selectedDateMillis?.let { utc ->
                        val day = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        // Время суток сохраняется, меняется только дата.
                        val newDate = day.atTime(local.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
                        vm.update { it.copy(date = newDate) }
                    }
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Отмена") } },
        ) { DatePicker(pickerState) }
    }

    record?.let { isRecord ->
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text(if (isRecord) "Новый рекорд!" else "Сохранено") },
            text = { Text(if (isRecord) "Результат лучше прежнего. Запись добавлена в журнал." else "Запись сохранена в журнал.") },
            confirmButton = { TextButton(onClick = onDone) { Text("Готово") } },
        )
    }
}
