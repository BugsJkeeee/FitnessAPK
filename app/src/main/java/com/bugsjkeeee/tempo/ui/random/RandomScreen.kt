package com.bugsjkeeee.tempo.ui.random

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.content.Exercise
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.content.WorkoutFilters
import com.bugsjkeeee.tempo.content.WorkoutFlags
import com.bugsjkeeee.tempo.content.pickRandom
import com.bugsjkeeee.tempo.content.randomCandidates
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.FilterPanel
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.WorkoutDetails
import com.bugsjkeeee.tempo.ui.components.plural
import com.bugsjkeeee.tempo.ui.launchWorkout
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RandomState(
    val loaded: Boolean = false,
    val filters: WorkoutFilters = WorkoutFilters(),
    val candidates: List<Workout> = emptyList(),
    val current: Workout? = null,
    /** Нажимали ли «Рандом» — до этого вместо результата подсказка. */
    val rolled: Boolean = false,
    val flags: Map<String, WorkoutFlags> = emptyMap(),
    val exercises: Map<String, Exercise> = emptyMap(),
)

class RandomViewModel(private val app: TempoApp) : ViewModel() {
    private val content = app.contentRepository
    private val filters = MutableStateFlow(WorkoutFilters())
    private val currentId = MutableStateFlow<String?>(null)
    private val rolled = MutableStateFlow(false)

    /** Тренировки, выполненные за последние 14 дней, не предлагаются. */
    private val recent = app.journalRepository.entries.map { list ->
        val from = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        list.filter { it.date >= from }.mapNotNull { it.workoutId }.toSet()
    }

    val state: StateFlow<RandomState> = combine(
        combine(content.workouts, content.flags, recent) { w, f, r -> Triple(w, f, r) },
        filters,
        currentId,
        rolled,
        flow { emit(content.exerciseMap()) },
    ) { (workouts, flags, recentIds), f, id, isRolled, exercises ->
        RandomState(
            loaded = true,
            filters = f,
            candidates = randomCandidates(workouts, f, flags, recentIds),
            current = workouts.firstOrNull { it.id == id },
            rolled = isRolled,
            flags = flags,
            exercises = exercises,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RandomState())

    fun setFilters(f: WorkoutFilters) {
        filters.value = f
    }

    fun roll() {
        rolled.value = true
        currentId.value = pickRandom(state.value.candidates, currentId.value)?.id
    }

    fun toggleFavorite(w: Workout) = viewModelScope.launch {
        val f = state.value.flags[w.id] ?: WorkoutFlags()
        content.setFlags(w.id, f.copy(favorite = !f.favorite))
    }

    fun hide(w: Workout) = viewModelScope.launch {
        val f = state.value.flags[w.id] ?: WorkoutFlags()
        content.setFlags(w.id, f.copy(hidden = true))
        currentId.value = pickRandom(state.value.candidates.filter { it.id != w.id }, w.id)?.id
    }

    /** По ТЗ фильтры сбрасываются при каждом открытии раздела. */
    fun reset() {
        filters.value = WorkoutFilters()
        currentId.value = null
        rolled.value = false
    }

    suspend fun launch(w: Workout): String = launchWorkout(app, w)
}

@Composable
fun RandomScreen(contentPadding: PaddingValues, onNavigate: (String) -> Unit, onExercise: (String) -> Unit) {
    val vm = appViewModel { RandomViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val style = LocalTempoStyle.current
    DisposableEffect(Unit) { onDispose { vm.reset() } }
    if (!state.loaded) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { FilterPanel(state.filters, vm::setFilters) }
        item {
            Button(
                onClick = vm::roll,
                enabled = state.candidates.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = style.tileShape,
            ) {
                Icon(Icons.Filled.Casino, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(if (state.rolled) "Ещё рандом" else "Рандом", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            Text(
                if (state.candidates.isEmpty()) {
                    "Под фильтры ничего не подошло — ослабьте фильтры. Тренировки за последние 14 дней и скрытые не предлагаются."
                } else {
                    "Подходит: " + plural(state.candidates.size, "тренировка", "тренировки", "тренировок")
                },
                style = MaterialTheme.typography.bodySmall,
                color = style.muted,
            )
        }
        val w = state.current
        if (w != null) {
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel(w.type.title)
                    Text(w.name, style = MaterialTheme.typography.headlineMedium)
                }
            }
            item { WorkoutDetails(w, state.exercises) { onExercise(it.id) } }
            item {
                Button(
                    onClick = { scope.launch { onNavigate(vm.launch(w)) } },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = style.tileShape,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Начать", style = MaterialTheme.typography.titleLarge)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::roll, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Другой")
                    }
                    val fav = state.flags[w.id]?.favorite == true
                    OutlinedButton(onClick = { vm.toggleFavorite(w) }, modifier = Modifier.weight(1f)) {
                        Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (fav) "В избранном" else "В избранное")
                    }
                }
            }
            item {
                OutlinedButton(onClick = { vm.hide(w) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Block, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Больше не предлагать")
                }
            }
        } else if (!state.rolled) {
            item {
                Text(
                    "Выберите фильтры или просто нажмите «Рандом» — приложение подберёт тренировку из базы.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = style.muted,
                )
            }
        }
    }
}
