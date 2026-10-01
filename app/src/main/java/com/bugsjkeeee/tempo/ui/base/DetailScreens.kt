package com.bugsjkeeee.tempo.ui.base

import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.content.Exercise
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.content.WorkoutFlags
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.ExerciseImage
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.WorkoutDetails
import com.bugsjkeeee.tempo.ui.launchWorkout
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Верхняя панель вложенных экранов со стрелкой «Назад». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(TempoIcons.Back, contentDescription = "Назад") } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

data class WorkoutDetailState(
    val loaded: Boolean = false,
    val workout: Workout? = null,
    val flags: WorkoutFlags = WorkoutFlags(),
    val exercises: Map<String, Exercise> = emptyMap(),
)

class WorkoutDetailViewModel(private val app: TempoApp, private val id: String) : ViewModel() {
    private val content = app.contentRepository

    val state: StateFlow<WorkoutDetailState> = combine(content.workouts, content.flags, flow { emit(content.exerciseMap()) }) { w, f, ex ->
        WorkoutDetailState(true, w.firstOrNull { it.id == id }, f[id] ?: WorkoutFlags(), ex)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailState())

    fun setFlags(flags: WorkoutFlags) {
        viewModelScope.launch { content.setFlags(id, flags) }
    }

    suspend fun delete() = content.deleteUserWorkout(id)
    suspend fun launch(w: Workout): String = launchWorkout(app, w)
}

@Composable
fun WorkoutDetailScreen(
    id: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onExercise: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    val vm = appViewModel { WorkoutDetailViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val style = LocalTempoStyle.current
    val w = state.workout

    Scaffold(topBar = {
        BackTopBar(w?.name ?: "", onBack) {
            if (w?.custom == true) {
                IconButton(onClick = { onEdit(w.id) }) { Icon(TempoIcons.Edit, contentDescription = "Редактировать") }
                IconButton(onClick = { confirmDelete = true }) { Icon(TempoIcons.Trash, contentDescription = "Удалить") }
            }
        }
    }) { padding ->
        if (w == null) {
            if (state.loaded) Text("Тренировка не найдена", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { WorkoutDetails(w, state.exercises) { onExercise(it.id) } }
            item {
                Button(
                    onClick = { scope.launch { onNavigate(vm.launch(w)) } },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = style.tileShape,
                ) {
                    Icon(TempoIcons.Play, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Начать", style = MaterialTheme.typography.titleLarge)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fav = state.flags.favorite
                    OutlinedButton(onClick = { vm.setFlags(state.flags.copy(favorite = !fav)) }, modifier = Modifier.weight(1f)) {
                        Icon(if (fav) TempoIcons.HeartFilled else TempoIcons.Heart, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (fav) "В избранном" else "В избранное")
                    }
                    val hidden = state.flags.hidden
                    OutlinedButton(onClick = { vm.setFlags(state.flags.copy(hidden = !hidden)) }, modifier = Modifier.weight(1f)) {
                        Icon(if (hidden) TempoIcons.Eye else TempoIcons.Ban, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (hidden) "Предлагать" else "Не предлагать")
                    }
                }
            }
        }
    }

    if (confirmDelete && w != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить тренировку?") },
            text = { Text("«${w.name}» будет удалена. Записи в журнале сохранятся.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        vm.delete()
                        onBack()
                    }
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

class ExerciseDetailViewModel(app: TempoApp, id: String) : ViewModel() {
    val exercise: StateFlow<Exercise?> = flow { emit(app.contentRepository.exerciseMap()[id]) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ExerciseDetailScreen(id: String, onBack: () -> Unit) {
    val vm = appViewModel { ExerciseDetailViewModel(it, id) }
    val ex by vm.exercise.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(topBar = { BackTopBar(ex?.name ?: "", onBack) }) { padding ->
        val e = ex ?: return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (e.images.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    e.images.forEach { ExerciseImage(it, Modifier.weight(1f).aspectRatio(0.85f)) }
                }
            }
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Мышцы")
                Text(e.muscles.joinToString(", ") { it.title }, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                TileLabel("Оборудование")
                Text(e.equipment.joinToString(", ") { it.title }, style = MaterialTheme.typography.bodyLarge)
            }
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Техника")
                Text(e.technique, style = MaterialTheme.typography.bodyLarge)
            }
            OutlinedButton(
                onClick = {
                    val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode("${e.name} техника"))
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(TempoIcons.Youtube, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Посмотреть на YouTube")
            }
        }
    }
}

class ExercisePickerViewModel(app: TempoApp) : ViewModel() {
    val exercises: StateFlow<List<Exercise>> = flow { emit(app.contentRepository.exercises().sortedBy { it.name }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Выбор упражнения: результат возвращается предыдущему экрану через savedStateHandle. */
@Composable
fun ExercisePickerScreen(onPicked: (String) -> Unit, onBack: () -> Unit) {
    val vm = appViewModel { ExercisePickerViewModel(it) }
    val list by vm.exercises.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Выбор упражнения", onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            ExerciseList(list, PaddingValues(16.dp), onPicked)
        }
    }
}
