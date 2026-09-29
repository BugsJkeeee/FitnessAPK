package com.bugsjkeeee.fitness.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.fitness.data.FitnessRepository
import com.bugsjkeeee.fitness.data.SetWithInfo
import com.bugsjkeeee.fitness.data.Workout
import com.bugsjkeeee.fitness.data.formatWeight
import com.bugsjkeeee.fitness.ui.appViewModel
import com.bugsjkeeee.fitness.ui.components.formatDuration
import com.bugsjkeeee.fitness.ui.components.parseDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val DEFAULT_REST_SECONDS = 90

data class ExerciseBlock(val exerciseOrder: Int, val exerciseId: Long, val name: String, val sets: List<SetWithInfo>)

sealed interface ActiveWorkoutState {
    data object Loading : ActiveWorkoutState
    data object None : ActiveWorkoutState
    data class Active(val workout: Workout, val blocks: List<ExerciseBlock>) : ActiveWorkoutState
}

/** Таймер отдыха: момент окончания и полная длительность (для индикатора). */
data class RestTimer(val endsAt: Long, val totalMillis: Long)

class ActiveWorkoutViewModel(private val repo: FitnessRepository) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ActiveWorkoutState> = repo.activeWorkout
        .flatMapLatest { workout ->
            if (workout == null) {
                flowOf(ActiveWorkoutState.None)
            } else {
                repo.setsForWorkout(workout.id).map { sets ->
                    ActiveWorkoutState.Active(
                        workout = workout,
                        blocks = sets.groupBy { it.exerciseOrder }.map { (order, list) ->
                            ExerciseBlock(order, list.first().exerciseId, list.first().exerciseName, list)
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutState.Loading)

    private val _rest = MutableStateFlow<RestTimer?>(null)
    val rest: StateFlow<RestTimer?> = _rest.asStateFlow()

    fun addExercise(workoutId: Long, exerciseId: Long) = viewModelScope.launch {
        repo.addExerciseToWorkout(workoutId, exerciseId)
    }

    fun addSet(workoutId: Long, exerciseOrder: Int) = viewModelScope.launch { repo.addSet(workoutId, exerciseOrder) }

    fun updateSet(id: Long, weight: Double, reps: Int, done: Boolean) = viewModelScope.launch {
        repo.updateSet(id, weight, reps, done)
    }

    fun toggleDone(set: SetWithInfo, weight: Double, reps: Int) {
        val done = !set.done
        updateSet(set.id, weight, reps, done)
        if (done) startRest(DEFAULT_REST_SECONDS)
    }

    fun deleteSet(id: Long) = viewModelScope.launch { repo.deleteSet(id) }

    fun removeExercise(workoutId: Long, exerciseOrder: Int) = viewModelScope.launch {
        repo.removeExerciseFromWorkout(workoutId, exerciseOrder)
    }

    fun rename(workout: Workout, name: String) = viewModelScope.launch { repo.renameWorkout(workout, name) }

    suspend fun finish(workout: Workout): Boolean {
        _rest.value = null
        return repo.finishWorkout(workout)
    }

    suspend fun cancel(workout: Workout) {
        _rest.value = null
        repo.deleteWorkout(workout.id)
    }

    fun startRest(seconds: Int) {
        _rest.value = RestTimer(System.currentTimeMillis() + seconds * 1000L, seconds * 1000L)
    }

    fun adjustRest(deltaSeconds: Int) {
        val timer = _rest.value ?: return
        val endsAt = timer.endsAt + deltaSeconds * 1000L
        _rest.value = if (endsAt <= System.currentTimeMillis()) {
            null
        } else {
            timer.copy(endsAt = endsAt, totalMillis = (timer.totalMillis + deltaSeconds * 1000L).coerceAtLeast(1000L))
        }
    }

    fun skipRest() {
        _rest.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    pickedExerciseId: Long?,
    onPickedConsumed: () -> Unit,
    onAddExercise: () -> Unit,
    onClose: () -> Unit,
) {
    val vm = appViewModel { ActiveWorkoutViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val rest by vm.rest.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }

    // По окончании отдыха — короткая вибрация и скрытие таймера.
    LaunchedEffect(rest, now) {
        val timer = rest ?: return@LaunchedEffect
        if (now >= timer.endsAt) {
            vm.skipRest()
            vibrate(context)
        }
    }

    val active = state as? ActiveWorkoutState.Active
    LaunchedEffect(state) {
        if (state is ActiveWorkoutState.None) onClose()
    }
    LaunchedEffect(pickedExerciseId, active?.workout?.id) {
        if (pickedExerciseId != null && active != null) {
            vm.addExercise(active.workout.id, pickedExerciseId)
            onPickedConsumed()
        }
    }

    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            active?.workout?.name ?: "",
                            modifier = Modifier.padding(end = 8.dp),
                            maxLines = 1,
                        )
                        active?.let {
                            Text(
                                formatDuration(now - it.workout.startedAt),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Свернуть") }
                },
                actions = {
                    TextButton(onClick = { renaming = true }) { Text("Имя") }
                    Button(onClick = { confirmFinish = true }, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Завершить")
                    }
                },
            )
        },
        bottomBar = {
            rest?.let { timer ->
                RestTimerBar(
                    timer = timer,
                    now = now,
                    onAdjust = vm::adjustRest,
                    onSkip = vm::skipRest,
                )
            }
        },
    ) { padding ->
        if (active == null) return@Scaffold
        val workoutId = active.workout.id
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(active.blocks, key = { it.exerciseOrder }) { block ->
                ExerciseCard(
                    block = block,
                    onAddSet = { vm.addSet(workoutId, block.exerciseOrder) },
                    onRemoveExercise = { vm.removeExercise(workoutId, block.exerciseOrder) },
                    onUpdate = vm::updateSet,
                    onToggleDone = vm::toggleDone,
                    onDeleteSet = vm::deleteSet,
                )
            }
            item {
                Button(onClick = onAddExercise, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Упражнение")
                }
            }
            item {
                TextButton(
                    onClick = { confirmCancel = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Отменить тренировку", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (active != null && confirmFinish) {
        val doneCount = active.blocks.sumOf { b -> b.sets.count { it.done } }
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Завершить тренировку?") },
            text = {
                Text(
                    if (doneCount == 0) {
                        "Нет выполненных подходов — тренировка не будет сохранена."
                    } else {
                        "Выполнено подходов: $doneCount. Неотмеченные подходы будут удалены."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    scope.launch { vm.finish(active.workout) }
                }) { Text("Завершить") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Продолжить") } },
        )
    }

    if (active != null && confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Отменить тренировку?") },
            text = { Text("Все записанные подходы этой тренировки будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    scope.launch { vm.cancel(active.workout) }
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Нет") } },
        )
    }

    if (active != null && renaming) {
        var name by rememberSaveable { mutableStateOf(active.workout.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Название тренировки") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    renaming = false
                    vm.rename(active.workout, name.trim())
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ExerciseCard(
    block: ExerciseBlock,
    onAddSet: () -> Unit,
    onRemoveExercise: () -> Unit,
    onUpdate: (Long, Double, Int, Boolean) -> Unit,
    onToggleDone: (SetWithInfo, Double, Int) -> Unit,
    onDeleteSet: (Long) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    block.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Меню упражнения")
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Удалить упражнение") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                menu = false
                                onRemoveExercise()
                            },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderCell("№", Modifier.width(32.dp))
                HeaderCell("кг", Modifier.weight(1f))
                HeaderCell("повт.", Modifier.weight(1f))
                Spacer(Modifier.width(96.dp))
            }
            block.sets.forEachIndexed { index, set ->
                SetRow(
                    number = index + 1,
                    set = set,
                    canDelete = block.sets.size > 1,
                    onUpdate = onUpdate,
                    onToggleDone = onToggleDone,
                    onDelete = { onDeleteSet(set.id) },
                )
            }
            OutlinedButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Подход")
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SetRow(
    number: Int,
    set: SetWithInfo,
    canDelete: Boolean,
    onUpdate: (Long, Double, Int, Boolean) -> Unit,
    onToggleDone: (SetWithInfo, Double, Int) -> Unit,
    onDelete: () -> Unit,
) {
    var weightText by rememberSaveable(set.id) { mutableStateOf(if (set.weight > 0) formatWeight(set.weight) else "") }
    var repsText by rememberSaveable(set.id) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    val weight = parseDecimal(weightText) ?: 0.0
    val reps = repsText.toIntOrNull() ?: 0
    val rowColor = if (set.done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(rowColor, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            number.toString(),
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        NumberField(
            value = weightText,
            onValueChange = { text ->
                weightText = text
                onUpdate(set.id, parseDecimal(text) ?: 0.0, reps, set.done)
            },
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
        NumberField(
            value = repsText,
            onValueChange = { text ->
                val digits = text.filter(Char::isDigit).take(3)
                repsText = digits
                onUpdate(set.id, weight, digits.toIntOrNull() ?: 0, set.done)
            },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        FilledIconToggleButton(
            checked = set.done,
            onCheckedChange = { onToggleDone(set, weight, reps) },
            enabled = reps > 0,
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                checkedContainerColor = MaterialTheme.colorScheme.secondary,
                checkedContentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Icon(Icons.Filled.Check, contentDescription = "Подход выполнен")
        }
        IconButton(onClick = onDelete, enabled = canDelete) {
            Icon(Icons.Filled.Close, contentDescription = "Удалить подход")
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle.Default.merge(MaterialTheme.typography.titleMedium).copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
    )
}

@Composable
private fun RestTimerBar(timer: RestTimer, now: Long, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val remaining = (timer.endsAt - now).coerceAtLeast(0)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, tonalElevation = 3.dp) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            LinearProgressIndicator(
                progress = { (remaining.toFloat() / timer.totalMillis).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Отдых", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatDuration(remaining + 999),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                TextButton(onClick = { onAdjust(-15) }) { Text("−15") }
                TextButton(onClick = { onAdjust(15) }) { Text("+15") }
                TextButton(onClick = onSkip) { Text("Пропустить") }
            }
        }
    }
}

private fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
}
