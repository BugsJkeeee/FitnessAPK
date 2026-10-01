package com.bugsjkeeee.tempo.ui.execute

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.content.Exercise
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.data.JournalEntry
import com.bugsjkeeee.tempo.data.JournalSet
import com.bugsjkeeee.tempo.data.isNewRecord
import com.bugsjkeeee.tempo.sound.SoundEvent
import com.bugsjkeeee.tempo.sound.SoundCatalog
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.base.BackTopBar
import com.bugsjkeeee.tempo.ui.components.ExerciseImage
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.formatClock
import com.bugsjkeeee.tempo.ui.components.formatRemaining
import com.bugsjkeeee.tempo.ui.components.parseDecimal
import com.bugsjkeeee.tempo.ui.theme.Digits
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val REST_SECONDS = 120

data class SetRow(val weight: String = "", val reps: String = "", val done: Boolean = false)

data class ExerciseBlock(val exercise: Exercise?, val exerciseId: String, val targetReps: Int?, val sets: List<SetRow>)

data class ExecuteState(
    val loaded: Boolean = false,
    val workout: Workout? = null,
    val blocks: List<ExerciseBlock> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    /** Окончание отдыха (мс) и его полная длительность. */
    val restEndsAt: Long? = null,
    val restTotalMs: Long = REST_SECONDS * 1000L,
)

class ExecuteViewModel(private val app: TempoApp, private val workoutId: String) : ViewModel() {
    private val _state = MutableStateFlow(ExecuteState())
    val state: StateFlow<ExecuteState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val w = app.contentRepository.workouts.first().firstOrNull { it.id == workoutId }
            val ex = app.contentRepository.exerciseMap()
            _state.value = ExecuteState(
                loaded = true,
                workout = w,
                blocks = w?.items.orEmpty().map { item ->
                    ExerciseBlock(ex[item.exercise], item.exercise, item.reps, List(item.sets ?: 3) { SetRow() })
                },
            )
        }
    }

    private fun updateSet(b: Int, s: Int, transform: (SetRow) -> SetRow) = _state.update { st ->
        st.copy(blocks = st.blocks.mapIndexed { bi, block ->
            if (bi != b) block else block.copy(sets = block.sets.mapIndexed { si, row -> if (si == s) transform(row) else row })
        })
    }

    fun setWeight(b: Int, s: Int, v: String) = updateSet(b, s) { it.copy(weight = v) }
    fun setReps(b: Int, s: Int, v: String) = updateSet(b, s) { it.copy(reps = v.filter(Char::isDigit).take(3)) }

    /** Отметка подхода: запускает отдых и переносит вес в следующий подход, если там пусто. */
    fun toggleDone(b: Int, s: Int) {
        val row = _state.value.blocks[b].sets[s]
        val done = !row.done
        updateSet(b, s) { it.copy(done = done) }
        if (done) {
            val next = _state.value.blocks[b].sets.getOrNull(s + 1)
            if (next != null && next.weight.isBlank()) updateSet(b, s + 1) { it.copy(weight = row.weight) }
            _state.update { it.copy(restEndsAt = System.currentTimeMillis() + REST_SECONDS * 1000L, restTotalMs = REST_SECONDS * 1000L) }
        }
    }

    /** Новый подход берёт вес предыдущего, повторы пустые. */
    fun addSet(b: Int) = _state.update { st ->
        st.copy(blocks = st.blocks.mapIndexed { bi, block ->
            if (bi != b) block else block.copy(sets = block.sets + SetRow(weight = block.sets.lastOrNull()?.weight.orEmpty()))
        })
    }

    fun adjustRest(deltaSec: Int) = _state.update { st ->
        val end = st.restEndsAt ?: return@update st
        val newEnd = end + deltaSec * 1000L
        if (newEnd <= System.currentTimeMillis()) st.copy(restEndsAt = null)
        else st.copy(restEndsAt = newEnd, restTotalMs = (st.restTotalMs + deltaSec * 1000L).coerceAtLeast(1000L))
    }

    fun skipRest() = _state.update { it.copy(restEndsAt = null) }

    fun restFinished() {
        skipRest()
        viewModelScope.launch {
            val settings = app.settingsRepository.current()
            app.audioPlayer.play(SoundCatalog.resolve(SoundEvent.WORK, settings.sounds[SoundEvent.WORK]).key)
        }
    }

    val doneCount: Int get() = _state.value.blocks.sumOf { b -> b.sets.count { it.done } }

    /** Сохраняет выполненные подходы в журнал; возвращает, побит ли рекорд. */
    suspend fun finish(): Boolean {
        val st = _state.value
        val w = st.workout ?: return false
        val sets = st.blocks.flatMap { block ->
            block.sets.filter { it.done }.mapIndexed { i, row ->
                JournalSet(block.exerciseId, i + 1, parseDecimal(row.weight), row.reps.toIntOrNull() ?: block.targetReps)
            }
        }
        val entry = JournalEntry(
            date = st.startedAt,
            workoutId = w.id,
            title = w.name,
            format = w.format,
            durationMs = System.currentTimeMillis() - st.startedAt,
            sets = sets,
        )
        val previous = app.journalRepository.all()
        app.journalRepository.save(entry)
        return isNewRecord(entry, previous)
    }
}

@Composable
fun ExecuteScreen(workoutId: String, onClose: () -> Unit) {
    val vm = appViewModel { ExecuteViewModel(it, workoutId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val style = LocalTempoStyle.current
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    var savedRecord by rememberSaveable { mutableStateOf<Boolean?>(null) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    LaunchedEffect(state.restEndsAt, now) {
        val end = state.restEndsAt ?: return@LaunchedEffect
        if (now >= end) vm.restFinished()
    }
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    BackHandler { confirmExit = true }

    Scaffold(
        topBar = {
            BackTopBar(state.workout?.name ?: "", onBack = { confirmExit = true }) {
                Text(formatClock(now - state.startedAt), style = Digits.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 16.dp))
            }
        },
        bottomBar = {
            val end = state.restEndsAt
            if (end != null) {
                val remaining = (end - now).coerceAtLeast(0)
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        LinearProgressIndicator(
                            progress = { (remaining.toFloat() / state.restTotalMs).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Отдых", style = MaterialTheme.typography.labelMedium)
                                Text(formatRemaining(remaining), style = Digits.copy(fontSize = 32.sp), color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = { vm.adjustRest(-15) }) { Text("−15") }
                            TextButton(onClick = { vm.adjustRest(15) }) { Text("+15") }
                            TextButton(onClick = vm::skipRest) { Text("Пропустить") }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(state.blocks) { b, block ->
                Tile(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExerciseImage(block.exercise?.images?.firstOrNull(), Modifier.size(44.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(block.exercise?.name ?: block.exerciseId, style = MaterialTheme.typography.titleMedium)
                            block.targetReps?.let { Text("цель: $it повт.", style = MaterialTheme.typography.bodySmall, color = style.muted) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    block.sets.forEachIndexed { s, row ->
                        SetRowView(
                            number = s + 1,
                            row = row,
                            onWeight = { vm.setWeight(b, s, it) },
                            onReps = { vm.setReps(b, s, it) },
                            onToggle = { vm.toggleDone(b, s) },
                        )
                    }
                    OutlinedButton(onClick = { vm.addSet(b) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Подход")
                    }
                }
            }
            item {
                Button(
                    onClick = { confirmFinish = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = style.tileShape,
                ) { Text("Завершить тренировку", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Завершить тренировку?") },
            text = { Text("Выполнено подходов: ${vm.doneCount}. Тренировка сохранится в журнал.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    scope.launch { savedRecord = vm.finish() }
                }) { Text("Завершить") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Продолжить") } },
        )
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Выйти без сохранения?") },
            text = { Text("Записанные подходы не попадут в журнал. Чтобы сохранить, нажмите «Завершить тренировку».") },
            confirmButton = { TextButton(onClick = { confirmExit = false; onClose() }) { Text("Выйти") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Остаться") } },
        )
    }
    savedRecord?.let { record ->
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text(if (record) "Новый рекорд!" else "Тренировка сохранена") },
            text = { Text(if (record) "Вы подняли больше, чем раньше. Тренировка сохранена в журнал." else "Запись добавлена в журнал.") },
            confirmButton = { TextButton(onClick = onClose) { Text("Готово") } },
        )
    }
}

@Composable
private fun SetRowView(number: Int, row: SetRow, onWeight: (String) -> Unit, onReps: (String) -> Unit, onToggle: () -> Unit) {
    val bg = if (row.done) LocalTempoStyle.current.positive.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent
    Row(
        Modifier.fillMaxWidth().background(bg, LocalTempoStyle.current.tileShape).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("$number", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = row.weight,
            onValueChange = onWeight,
            label = { Text("кг") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = Digits.copy(fontSize = 18.sp),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = row.reps,
            onValueChange = onReps,
            label = { Text("повт.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = Digits.copy(fontSize = 18.sp),
            modifier = Modifier.weight(1f),
        )
        FilledIconToggleButton(
            checked = row.done,
            onCheckedChange = { onToggle() },
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                checkedContainerColor = LocalTempoStyle.current.positive,
            ),
        ) { Icon(Icons.Filled.Check, contentDescription = "Подход выполнен") }
    }
}

