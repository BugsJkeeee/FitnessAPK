package com.bugsjkeeee.tempo.ui.random

import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bugsjkeeee.tempo.ui.components.Note
import com.bugsjkeeee.tempo.ui.components.PrimaryButton
import com.bugsjkeeee.tempo.ui.components.SecondaryButton
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
    /** Нажимали ли «Новая тренировка» — до этого вместо результата подсказка. */
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

    private var handledReset = 0

    /** По ТЗ фильтры сбрасываются при каждом открытии раздела; [key] растёт при переходе на вкладку. */
    fun resetIfNew(key: Int) {
        if (key == handledReset) return
        handledReset = key
        reset()
    }

    fun reset() {
        filters.value = WorkoutFilters()
        currentId.value = null
        rolled.value = false
    }

    suspend fun launch(w: Workout): String = launchWorkout(app, w)
}

@Composable
fun RandomScreen(contentPadding: PaddingValues, resetKey: Int, onNavigate: (String) -> Unit, onExercise: (String) -> Unit) {
    val vm = appViewModel { RandomViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Сброс только при новом открытии вкладки, а не после возврата с экрана упражнения.
    LaunchedEffect(resetKey) { vm.resetIfNew(resetKey) }
    if (!state.loaded) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { FilterPanel(state.filters, vm::setFilters) }
        item {
            PrimaryButton(
                "Новая тренировка",
                onClick = vm::roll,
                icon = TempoIcons.Random,
                enabled = state.candidates.isNotEmpty(),
                height = 58.dp,
            )
        }
        item {
            Note(
                if (state.candidates.isEmpty()) {
                    "Под фильтры ничего не подошло — ослабьте фильтры. Тренировки за последние 14 дней и скрытые не предлагаются."
                } else {
                    "Подходит: " + plural(state.candidates.size, "тренировка", "тренировки", "тренировок")
                },
                Modifier.padding(horizontal = 4.dp),
            )
        }
        val w = state.current
        if (w != null) {
            item {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    TileLabel("Ваша тренировка · " + w.type.title, TempoIcons.Random)
                    Text(w.name, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            item { WorkoutDetails(w, state.exercises) { onExercise(it.id) } }
            item {
                PrimaryButton("Начать", onClick = { scope.launch { onNavigate(vm.launch(w)) } }, icon = TempoIcons.Play)
            }
            item {
                val fav = state.flags[w.id]?.favorite == true
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Другой", vm::roll, Modifier.weight(1f), TempoIcons.Refresh)
                    SecondaryButton(
                        if (fav) "В избранном" else "В избранное",
                        { vm.toggleFavorite(w) },
                        Modifier.weight(1f),
                        if (fav) TempoIcons.HeartFilled else TempoIcons.Heart,
                        color = if (fav) MaterialTheme.colorScheme.primary else null,
                    )
                }
            }
            item { SecondaryButton("Больше не предлагать", { vm.hide(w) }, Modifier.fillMaxWidth(), TempoIcons.Ban) }
        }
    }
}
