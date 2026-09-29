package com.bugsjkeeee.fitness.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.fitness.data.Exercise
import com.bugsjkeeee.fitness.data.FitnessRepository
import com.bugsjkeeee.fitness.data.ProgressPoint
import com.bugsjkeeee.fitness.data.formatWeight
import com.bugsjkeeee.fitness.data.progressPoints
import com.bugsjkeeee.fitness.ui.TabScreenInsets
import com.bugsjkeeee.fitness.ui.appViewModel
import com.bugsjkeeee.fitness.ui.components.ChartPoint
import com.bugsjkeeee.fitness.ui.components.EmptyState
import com.bugsjkeeee.fitness.ui.components.LineChart
import com.bugsjkeeee.fitness.ui.components.SectionTitle
import com.bugsjkeeee.fitness.ui.components.StatCard
import com.bugsjkeeee.fitness.ui.components.formatDate
import com.bugsjkeeee.fitness.ui.components.formatVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProgressState(
    val loaded: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    val selected: Exercise? = null,
    val points: List<ProgressPoint> = emptyList(),
)

class ProgressViewModel(repo: FitnessRepository) : ViewModel() {
    private val selectedId = MutableStateFlow<Long?>(null)

    val state: StateFlow<ProgressState> = combine(
        repo.exercisesWithHistory,
        repo.allFinishedSets,
        selectedId,
    ) { exercises, sets, selected ->
        // По умолчанию — упражнение из последней тренировки.
        val current = exercises.firstOrNull { it.id == selected }
            ?: sets.lastOrNull()?.let { last -> exercises.firstOrNull { it.id == last.exerciseId } }
            ?: exercises.firstOrNull()
        ProgressState(
            loaded = true,
            exercises = exercises,
            selected = current,
            points = current?.let { ex -> progressPoints(sets.filter { it.exerciseId == ex.id }) }.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun select(id: Long) {
        selectedId.value = id
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen() {
    val vm = appViewModel { ProgressViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = TabScreenInsets,
        topBar = { TopAppBar(title = { Text("Прогресс") }) },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        val selected = state.selected
        if (selected == null) {
            EmptyState(
                Icons.AutoMirrored.Filled.ShowChart,
                "Нет данных",
                "Графики появятся после первой завершённой тренировки.",
                Modifier.padding(padding),
            )
            return@Scaffold
        }
        val points = state.points
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ExerciseSelector(state.exercises, selected, vm::select) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Расчётный 1ПМ", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Оценка максимального веса на одно повторение: вес × (1 + повторения / 30)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        LineChart(points.map { ChartPoint(it.date, it.bestOneRepMax) })
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("${formatWeight(points.maxOf { it.maxWeight })} кг", "макс. вес", Modifier.weight(1f))
                    StatCard("${formatWeight(points.maxOf { it.bestOneRepMax })} кг", "лучший 1ПМ", Modifier.weight(1f))
                    StatCard(formatVolume(points.maxOf { it.volume }), "макс. тоннаж", Modifier.weight(1f))
                }
            }
            item { SectionTitle("По тренировкам") }
            items(points.reversed()) { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(formatDate(p.date), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${formatWeight(p.maxWeight)} кг · 1ПМ ${formatWeight(p.bestOneRepMax)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSelector(exercises: List<Exercise>, selected: Exercise, onSelect: (Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Упражнение") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise.name) },
                    onClick = {
                        expanded = false
                        onSelect(exercise.id)
                    },
                )
            }
        }
    }
}
