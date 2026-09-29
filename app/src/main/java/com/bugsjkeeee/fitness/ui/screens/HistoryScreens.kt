package com.bugsjkeeee.fitness.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.fitness.data.FitnessRepository
import com.bugsjkeeee.fitness.data.SetWithInfo
import com.bugsjkeeee.fitness.data.Workout
import com.bugsjkeeee.fitness.data.formatWeight
import com.bugsjkeeee.fitness.data.volume
import com.bugsjkeeee.fitness.ui.TabScreenInsets
import com.bugsjkeeee.fitness.ui.appViewModel
import com.bugsjkeeee.fitness.ui.components.EmptyState
import com.bugsjkeeee.fitness.ui.components.StatCard
import com.bugsjkeeee.fitness.ui.components.formatDate
import com.bugsjkeeee.fitness.ui.components.formatDuration
import com.bugsjkeeee.fitness.ui.components.formatVolume
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutSummary(val workout: Workout, val sets: List<SetWithInfo>) {
    val exerciseNames: List<String> get() = sets.map { it.exerciseName }.distinct()
    val duration: Long get() = (workout.finishedAt ?: workout.startedAt) - workout.startedAt
}

class HistoryViewModel(repo: FitnessRepository) : ViewModel() {
    val workouts: StateFlow<List<WorkoutSummary>?> = combine(repo.finishedWorkouts, repo.allFinishedSets) { workouts, sets ->
        val byWorkout = sets.groupBy { it.workoutId }
        workouts.map { WorkoutSummary(it, byWorkout[it.id].orEmpty()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onOpenWorkout: (Long) -> Unit) {
    val vm = appViewModel { HistoryViewModel(it) }
    val workouts by vm.workouts.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = TabScreenInsets,
        topBar = { TopAppBar(title = { Text("История") }) },
    ) { padding ->
        val list = workouts ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                Icons.Filled.History,
                "Пока пусто",
                "Завершённые тренировки появятся здесь.",
                Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(list, key = { it.workout.id }) { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenWorkout(summary.workout.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(summary.workout.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatDate(summary.workout.startedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${formatDuration(summary.duration)} · ${summary.sets.size} подх. · ${formatVolume(volume(summary.sets))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            summary.exerciseNames.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

class WorkoutDetailViewModel(private val repo: FitnessRepository, private val workoutId: Long) : ViewModel() {
    val summary: StateFlow<WorkoutSummary?> = combine(repo.workout(workoutId), repo.setsForWorkout(workoutId)) { w, sets ->
        w?.let { WorkoutSummary(it, sets) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun delete() = repo.deleteWorkout(workoutId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(workoutId: Long, onBack: () -> Unit) {
    val vm = appViewModel { WorkoutDetailViewModel(it, workoutId) }
    val summary by vm.summary.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(summary?.workout?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить тренировку")
                    }
                },
            )
        },
    ) { padding ->
        val s = summary ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(formatDate(s.workout.startedAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(formatDuration(s.duration), "длительность", Modifier.weight(1f))
                    StatCard(s.sets.size.toString(), "подходов", Modifier.weight(1f))
                    StatCard(formatVolume(volume(s.sets)), "тоннаж", Modifier.weight(1f))
                }
            }
            items(s.sets.groupBy { it.exerciseOrder }.values.toList()) { sets ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            sets.first().exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        sets.forEachIndexed { i, set ->
                            Text("${i + 1}.  ${formatWeight(set.weight)} кг × ${set.reps}")
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить тренировку?") },
            text = { Text("Тренировка и все её подходы будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        vm.delete()
                        onBack()
                    }
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}
