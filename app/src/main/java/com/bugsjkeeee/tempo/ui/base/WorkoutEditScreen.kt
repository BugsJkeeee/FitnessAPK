package com.bugsjkeeee.tempo.ui.base

import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import com.bugsjkeeee.tempo.ui.components.TButton
import com.bugsjkeeee.tempo.ui.components.TOutlinedButton
import com.bugsjkeeee.tempo.ui.components.TOutlinedTextField
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.content.Exercise
import com.bugsjkeeee.tempo.content.Level
import com.bugsjkeeee.tempo.content.TimerSpec
import com.bugsjkeeee.tempo.content.WorkoutFormat
import com.bugsjkeeee.tempo.content.WorkoutItem
import com.bugsjkeeee.tempo.content.deriveWorkout
import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.CountStepper
import com.bugsjkeeee.tempo.ui.components.DurationStepper
import com.bugsjkeeee.tempo.ui.components.SegmentedSelector
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.WorkoutCard
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Черновик своей тренировки в форме. Параметры таймера хранятся для всех форматов, используется нужный. */
data class WorkoutDraft(
    val loaded: Boolean = false,
    val name: String = "",
    val format: WorkoutFormat = WorkoutFormat.AMRAP,
    val level: Level = Level.MEDIUM,
    val capSec: Int = 15 * 60,
    val amrapSec: Int = 15 * 60,
    val emomIntervalSec: Int = 60,
    val emomRounds: Int = 12,
    val workSec: Int = 20,
    val restSec: Int = 10,
    val tabataRounds: Int = 8,
    val items: List<WorkoutItem> = emptyList(),
    val description: String = "",
    val exercises: Map<String, Exercise> = emptyMap(),
) {
    fun timer(): TimerSpec? = when (format) {
        WorkoutFormat.FOR_TIME -> TimerSpec(TimerMode.FOR_TIME, capSec = capSec)
        WorkoutFormat.AMRAP -> TimerSpec(TimerMode.AMRAP, durationSec = amrapSec)
        WorkoutFormat.EMOM -> TimerSpec(TimerMode.EMOM, intervalSec = emomIntervalSec, rounds = emomRounds)
        WorkoutFormat.TABATA -> TimerSpec(TimerMode.INTERVALS, workSec = workSec, restSec = restSec, rounds = tabataRounds)
        WorkoutFormat.SETS -> null
    }
}

class WorkoutEditViewModel(private val app: TempoApp, private val id: String?) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutDraft())
    val state: StateFlow<WorkoutDraft> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val exercises = app.contentRepository.exerciseMap()
            val w = id?.let { wid -> app.contentRepository.workouts.first().firstOrNull { it.id == wid } }
            val t = w?.timer
            _state.value = if (w == null) {
                WorkoutDraft(loaded = true, exercises = exercises)
            } else {
                WorkoutDraft(
                    loaded = true,
                    name = w.name,
                    format = w.format,
                    level = Level.of(w.level),
                    capSec = t?.capSec ?: 15 * 60,
                    amrapSec = t?.durationSec ?: 15 * 60,
                    emomIntervalSec = t?.intervalSec ?: 60,
                    emomRounds = t?.rounds ?: 12,
                    workSec = t?.workSec ?: 20,
                    restSec = t?.restSec ?: 10,
                    tabataRounds = t?.rounds ?: 8,
                    items = w.items,
                    description = w.description,
                    exercises = exercises,
                )
            }
        }
    }

    fun update(transform: (WorkoutDraft) -> WorkoutDraft) = _state.update(transform)

    fun addExercise(exerciseId: String) = _state.update {
        val item = if (it.format == WorkoutFormat.SETS) WorkoutItem(exerciseId, sets = 3, reps = 10) else WorkoutItem(exerciseId, dose = "10")
        it.copy(items = it.items + item)
    }

    suspend fun save() {
        val d = _state.value
        // При смене формата приводим упражнения к нужному виду: подходы × повторы или дозировка текстом.
        val items = d.items.map {
            if (d.format == WorkoutFormat.SETS) it.copy(dose = null, sets = it.sets ?: 3, reps = it.reps ?: 10)
            else it.copy(sets = null, reps = null, dose = it.dose ?: it.reps?.toString() ?: "")
        }
        val workout = deriveWorkout(
            id = id ?: "u${System.currentTimeMillis()}",
            name = d.name.trim(),
            format = d.format,
            level = d.level.value,
            items = items,
            timer = d.timer(),
            description = d.description.trim(),
            exercises = d.exercises,
        )
        app.contentRepository.saveUserWorkout(workout)
    }
}

@Composable
fun WorkoutEditScreen(
    id: String?,
    pickedExercise: String?,
    onPickedConsumed: () -> Unit,
    onPickExercise: () -> Unit,
    onDone: () -> Unit,
) {
    val vm = appViewModel { WorkoutEditViewModel(it, id) }
    val d by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val style = LocalTempoStyle.current

    LaunchedEffect(pickedExercise, d.loaded) {
        if (pickedExercise != null && d.loaded) {
            vm.addExercise(pickedExercise)
            onPickedConsumed()
        }
    }

    Scaffold(topBar = { BackTopBar(if (id == null) "Своя тренировка" else "Редактирование", onDone) }) { padding ->
        if (!d.loaded) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TOutlinedTextField(
                    value = d.name,
                    onValueChange = { v -> vm.update { it.copy(name = v) } },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Формат")
                    Spacer(Modifier.height(6.dp))
                    SegmentedSelector(WorkoutFormat.entries, d.format, { it.title }, { f -> vm.update { it.copy(format = f) } })
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (d.format) {
                            WorkoutFormat.FOR_TIME -> DurationStepper("Лимит времени", d.capSec, { v -> vm.update { it.copy(capSec = v) } }, min = 60)
                            WorkoutFormat.AMRAP -> DurationStepper("Длительность", d.amrapSec, { v -> vm.update { it.copy(amrapSec = v) } }, min = 60)
                            WorkoutFormat.EMOM -> {
                                DurationStepper("Интервал", d.emomIntervalSec, { v -> vm.update { it.copy(emomIntervalSec = v) } })
                                CountStepper("Раунды", d.emomRounds, { v -> vm.update { it.copy(emomRounds = v) } })
                            }
                            WorkoutFormat.TABATA -> {
                                DurationStepper("Работа", d.workSec, { v -> vm.update { it.copy(workSec = v) } })
                                DurationStepper("Отдых", d.restSec, { v -> vm.update { it.copy(restSec = v) } }, min = 0, zeroText = "нет")
                                CountStepper("Раунды", d.tabataRounds, { v -> vm.update { it.copy(tabataRounds = v) } })
                            }
                            WorkoutFormat.SETS -> Text("Силовая тренировка: для каждого упражнения задайте подходы и повторы.", color = style.muted)
                        }
                    }
                }
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Сложность")
                    Spacer(Modifier.height(6.dp))
                    SegmentedSelector(Level.entries, d.level, { it.title }, { l -> vm.update { it.copy(level = l) } })
                }
            }
            item { TileLabel("Упражнения") }
            itemsIndexed(d.items) { index, item ->
                Tile(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.exercises[item.exercise]?.name ?: item.exercise, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.update { it.copy(items = it.items.filterIndexed { i, _ -> i != index }) } }) {
                            Icon(TempoIcons.Close, contentDescription = "Убрать")
                        }
                    }
                    fun set(new: WorkoutItem) = vm.update { it.copy(items = it.items.toMutableList().also { l -> l[index] = new }) }
                    if (d.format == WorkoutFormat.SETS) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CountStepper("Подходы", item.sets ?: 3, { set(item.copy(sets = it)) }, max = 20)
                            CountStepper("Повторы", item.reps ?: 10, { set(item.copy(reps = it)) }, max = 100)
                        }
                    } else {
                        TOutlinedTextField(
                            value = item.dose ?: "",
                            onValueChange = { set(item.copy(dose = it)) },
                            label = { Text("Дозировка, например «15» или «21-15-9, 40 кг»") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                TOutlinedButton(onClick = onPickExercise, modifier = Modifier.fillMaxWidth()) {
                    Icon(TempoIcons.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Упражнение")
                }
            }
            item {
                TOutlinedTextField(
                    value = d.description,
                    onValueChange = { v -> vm.update { it.copy(description = v) } },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            item {
                TButton(
                    onClick = { scope.launch { vm.save(); onDone() } },
                    enabled = d.name.isNotBlank() && d.items.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = style.tileShape,
                ) { Text("Сохранить", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

class WorkoutPickerViewModel(app: TempoApp) : ViewModel() {
    val workouts = app.contentRepository.workouts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Выбор тренировки из базы — для ручной записи в журнал. */
@Composable
fun WorkoutPickerScreen(onPicked: (String) -> Unit, onBack: () -> Unit) {
    val vm = appViewModel { WorkoutPickerViewModel(it) }
    val all by vm.workouts.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { BackTopBar("Выбор тренировки", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TOutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск по названию") },
                    leadingIcon = { Icon(TempoIcons.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(all.filter { it.name.contains(query.trim(), ignoreCase = true) }, key = { it.id }) { w ->
                WorkoutCard(w, favorite = false, onClick = { onPicked(w.id) })
            }
        }
    }
}
