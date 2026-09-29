package com.bugsjkeeee.fitness.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.fitness.data.FitnessRepository
import com.bugsjkeeee.fitness.data.Template
import com.bugsjkeeee.fitness.data.TemplateExerciseWithName
import com.bugsjkeeee.fitness.data.Workout
import com.bugsjkeeee.fitness.ui.TabScreenInsets
import com.bugsjkeeee.fitness.ui.appViewModel
import com.bugsjkeeee.fitness.ui.components.SectionTitle
import com.bugsjkeeee.fitness.ui.components.StatCard
import com.bugsjkeeee.fitness.ui.components.formatDateTime
import com.bugsjkeeee.fitness.ui.components.formatVolume
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class WeekStats(val workouts: Int = 0, val sets: Int = 0, val volume: Double = 0.0)

data class HomeState(
    val activeWorkout: Workout? = null,
    val week: WeekStats = WeekStats(),
    val templates: List<Pair<Template, List<TemplateExerciseWithName>>> = emptyList(),
)

class HomeViewModel(private val repo: FitnessRepository) : ViewModel() {
    val state: StateFlow<HomeState> = combine(
        repo.activeWorkout,
        repo.allFinishedSets,
        repo.templates,
        repo.allTemplateExercises,
    ) { active, sets, templates, templateExercises ->
        val weekStart = startOfWeek()
        val weekSets = sets.filter { (it.finishedAt ?: 0) >= weekStart }
        HomeState(
            activeWorkout = active,
            week = WeekStats(
                workouts = weekSets.map { it.workoutId }.distinct().size,
                sets = weekSets.size,
                volume = weekSets.sumOf { it.weight * it.reps },
            ),
            templates = templates.map { t -> t to templateExercises.filter { it.templateId == t.id } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    suspend fun start(templateId: Long?) = repo.startWorkout(templateId)

    private fun startOfWeek(): Long = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenWorkout: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onOpenExercises: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val start: (Long?) -> Unit = { templateId ->
        scope.launch {
            vm.start(templateId)
            onOpenWorkout()
        }
    }

    Scaffold(
        contentWindowInsets = TabScreenInsets,
        topBar = { TopAppBar(title = { Text("Тренировка") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.activeWorkout?.let { active ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenWorkout),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Тренировка идёт", style = MaterialTheme.typography.titleMedium)
                                Text("${active.name} · начата ${formatDateTime(active.startedAt)}")
                            }
                            Button(onClick = onOpenWorkout) { Text("Продолжить") }
                        }
                    }
                }
            }

            item { SectionTitle("Эта неделя") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(state.week.workouts.toString(), "тренировок", Modifier.weight(1f))
                    StatCard(state.week.sets.toString(), "подходов", Modifier.weight(1f))
                    StatCard(formatVolume(state.week.volume), "тоннаж", Modifier.weight(1f))
                }
            }

            if (state.activeWorkout == null) {
                item {
                    Button(
                        onClick = { start(null) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Пустая тренировка", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item { SectionTitle("Шаблоны") }
            if (state.templates.isEmpty()) {
                item {
                    Text(
                        "Сохраните свою программу как шаблон, чтобы начинать тренировку одним касанием.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.templates, key = { it.first.id }) { (template, exercises) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onEditTemplate(template.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(template.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                exercises.joinToString(" · ") { it.exerciseName }.ifEmpty { "Нет упражнений" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (state.activeWorkout == null) {
                            FilledTonalButton(onClick = { start(template.id) }) { Text("Начать") }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onEditTemplate(0L) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Новый шаблон")
                    }
                    OutlinedButton(onClick = onOpenExercises, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Упражнения")
                    }
                }
            }
        }
    }
}
