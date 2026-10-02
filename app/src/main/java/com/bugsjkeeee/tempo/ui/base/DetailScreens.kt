package com.bugsjkeeee.tempo.ui.base

import com.bugsjkeeee.tempo.ui.launchWarmup
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
import com.bugsjkeeee.tempo.ui.components.PrimaryButton
import com.bugsjkeeee.tempo.ui.components.RoundIconButton
import com.bugsjkeeee.tempo.ui.components.SecondaryButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
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
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(TempoIcons.Back, "Назад", onBack, tint = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
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
    val app = LocalContext.current.applicationContext as TempoApp
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val style = LocalTempoStyle.current
    val w = state.workout

    Scaffold(topBar = {
        BackTopBar(w?.name ?: "", onBack) {
            if (w?.custom == true) {
                RoundIconButton(TempoIcons.Edit, "Редактировать", { onEdit(w.id) })
                RoundIconButton(TempoIcons.Trash, "Удалить", { confirmDelete = true })
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
            item { WorkoutDetails(
                    w,
                    state.exercises,
                    onWarmup = { scope.launch { onNavigate(launchWarmup(app, w)) } },
                ) { onExercise(it.id) } }
            item {
                PrimaryButton("Начать", onClick = { scope.launch { onNavigate(vm.launch(w)) } }, icon = TempoIcons.Play)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fav = state.flags.favorite
                    SecondaryButton(
                        if (fav) "В избранном" else "В избранное",
                        { vm.setFlags(state.flags.copy(favorite = !fav)) },
                        Modifier.weight(1f),
                        if (fav) TempoIcons.HeartFilled else TempoIcons.Heart,
                        color = if (fav) MaterialTheme.colorScheme.primary else null,
                    )
                    val hidden = state.flags.hidden
                    SecondaryButton(
                        if (hidden) "Предлагать" else "Не предлагать",
                        { vm.setFlags(state.flags.copy(hidden = !hidden)) },
                        Modifier.weight(1f),
                        if (hidden) TempoIcons.Eye else TempoIcons.Ban,
                    )
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
                TileLabel("Мышцы", TempoIcons.Muscle)
                Text(e.muscles.joinToString(", ") { it.title }, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                TileLabel("Оборудование", TempoIcons.Dumbbell)
                Text(e.equipment.joinToString(", ") { it.title }, style = MaterialTheme.typography.bodyLarge)
            }
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Техника", TempoIcons.Clipboard)
                Text(e.technique, style = MaterialTheme.typography.bodyLarge)
            }
            SecondaryButton(
                "Посмотреть на YouTube",
                {
                    val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode("${e.name} техника"))
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                Modifier.fillMaxWidth(),
                TempoIcons.Youtube,
            )
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
