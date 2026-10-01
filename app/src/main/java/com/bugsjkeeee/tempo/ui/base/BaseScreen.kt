package com.bugsjkeeee.tempo.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.bugsjkeeee.tempo.content.Muscle
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.content.WorkoutFilters
import com.bugsjkeeee.tempo.content.WorkoutFlags
import com.bugsjkeeee.tempo.content.matches
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.ExerciseImage
import com.bugsjkeeee.tempo.ui.components.FilterPanel
import com.bugsjkeeee.tempo.ui.components.SegmentedSelector
import com.bugsjkeeee.tempo.ui.components.WorkoutCard
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class BaseState(
    val loaded: Boolean = false,
    val workouts: List<Workout> = emptyList(),
    val filtered: List<Workout> = emptyList(),
    val favorites: List<Workout> = emptyList(),
    val flags: Map<String, WorkoutFlags> = emptyMap(),
    val filters: WorkoutFilters = WorkoutFilters(),
    val exercises: List<Exercise> = emptyList(),
)

class BaseViewModel(app: TempoApp) : ViewModel() {
    private val content = app.contentRepository
    private val filters = MutableStateFlow(WorkoutFilters())

    val state: StateFlow<BaseState> = combine(content.workouts, content.flags, filters, content.exercisesFlow) { w, flags, f, ex ->
        BaseState(
            loaded = true,
            workouts = w,
            filtered = w.filter { it.matches(f, flags[it.id] ?: WorkoutFlags()) },
            favorites = w.filter { flags[it.id]?.favorite == true },
            flags = flags,
            filters = f,
            exercises = ex.sortedBy { it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BaseState())

    fun setFilters(f: WorkoutFilters) {
        filters.value = f
    }
}

private val baseTabs = listOf("Тренировки", "Упражнения", "Избранное")

@Composable
fun BaseScreen(
    contentPadding: PaddingValues,
    onWorkout: (String) -> Unit,
    onExercise: (String) -> Unit,
    onNewWorkout: () -> Unit,
) {
    val vm = appViewModel { BaseViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    if (!state.loaded) return

    Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                baseTabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            val listPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = contentPadding.calculateBottomPadding() + 88.dp)
            when (tab) {
                0 -> LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = state.filters.query,
                            onValueChange = { vm.setFilters(state.filters.copy(query = it)) },
                            placeholder = { Text("Поиск по названию") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item { FilterPanel(state.filters, vm::setFilters) }
                    item {
                        Text(
                            "Найдено: ${state.filtered.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalTempoStyle.current.muted,
                        )
                    }
                    items(state.filtered, key = { it.id }) { w ->
                        WorkoutCard(w, state.flags[w.id]?.favorite == true, onClick = { onWorkout(w.id) })
                    }
                }
                1 -> ExerciseList(state.exercises, listPadding, onExercise)
                else -> LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.favorites.isEmpty()) {
                        item {
                            Text(
                                "Отмечайте тренировки кнопкой «В избранное» — они соберутся здесь.",
                                color = LocalTempoStyle.current.muted,
                            )
                        }
                    }
                    items(state.favorites, key = { it.id }) { w ->
                        WorkoutCard(w, favorite = true, onClick = { onWorkout(w.id) })
                    }
                }
            }
        }
        if (tab == 0) {
            FloatingActionButton(
                onClick = onNewWorkout,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
            ) { Icon(Icons.Filled.Add, contentDescription = "Своя тренировка") }
        }
    }
}

/** Справочник упражнений с поиском и фильтром по группе мышц; используется и для выбора упражнения. */
@Composable
fun ExerciseList(exercises: List<Exercise>, contentPadding: PaddingValues, onClick: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<Muscle?>(null) }
    val style = LocalTempoStyle.current
    val shown = exercises.filter {
        (muscle == null || muscle in it.muscles) && it.name.contains(query.trim(), ignoreCase = true)
    }
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск упражнения") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SegmentedSelector(
                options = listOf<Muscle?>(null) + Muscle.entries,
                selected = muscle,
                label = { it?.title ?: "Все" },
                onSelect = { muscle = it },
                columns = 4,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        items(shown, key = { it.id }) { ex ->
            Row(
                Modifier.fillMaxWidth().clickable { onClick(ex.id) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExerciseImage(ex.images.firstOrNull(), Modifier.size(56.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        ex.muscles.joinToString(", ") { it.title } + " · " + ex.equipment.joinToString(", ") { it.title },
                        style = MaterialTheme.typography.bodySmall,
                        color = style.muted,
                    )
                }
            }
        }
    }
}
