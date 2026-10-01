package com.bugsjkeeee.tempo.ui.journal

import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.data.JournalEntry
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.base.BackTopBar
import com.bugsjkeeee.tempo.ui.components.InfoTile
import com.bugsjkeeee.tempo.ui.components.LineChart
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.formatClock
import com.bugsjkeeee.tempo.ui.components.formatDate
import com.bugsjkeeee.tempo.ui.components.formatDateShort
import com.bugsjkeeee.tempo.ui.components.formatDateTime
import com.bugsjkeeee.tempo.ui.components.formatWeight
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.launch

/** Подходы записи, сгруппированные по упражнениям с сохранением порядка. */
@Composable
fun SetsByExercise(entry: JournalEntry, names: (String) -> String) {
    val style = LocalTempoStyle.current
    entry.sets.groupBy { it.exerciseId }.forEach { (exerciseId, sets) ->
        Tile(Modifier.fillMaxWidth()) {
            Text(names(exerciseId), style = MaterialTheme.typography.titleMedium)
            sets.forEach { s ->
                val parts = listOfNotNull(s.weight?.let { "${formatWeight(it)} кг" }, s.reps?.let { "$it повт." })
                Text("${s.setIndex}.  " + parts.joinToString(" × ").ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge, color = style.muted)
            }
        }
    }
}

@Composable
fun EntryScreen(id: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val vm = appViewModel { JournalViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as TempoApp
    val scope = rememberCoroutineScope()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val entry = state.entries.firstOrNull { it.id == id }

    Scaffold(topBar = {
        BackTopBar(entry?.title ?: "", onBack) {
            IconButton(onClick = { onEdit(id) }) { Icon(TempoIcons.Edit, contentDescription = "Редактировать") }
            IconButton(onClick = { confirmDelete = true }) { Icon(TempoIcons.Trash, contentDescription = "Удалить") }
        }
    }) { padding ->
        val e = entry ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(formatDateTime(e.date), color = LocalTempoStyle.current.muted) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoTile("Формат", e.format?.title ?: e.timerMode?.title ?: "—", Modifier.weight(1f))
                    InfoTile("Длительность", e.durationMs?.let(::formatClock) ?: "—", Modifier.weight(1f))
                }
            }
            val result = e.resultText()
            if (result.isNotEmpty()) item { InfoTile("Результат", result, Modifier.fillMaxWidth()) }
            item { SetsByExercise(e) { state.exercises[it]?.name ?: it } }
            if (e.note.isNotBlank()) item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Заметка")
                    Text(e.note, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить запись?") },
            text = { Text("Запись и её подходы будут удалены из журнала.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch { app.journalRepository.delete(id); onBack() }
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

@Composable
fun ExerciseRecordScreen(exerciseId: String, onBack: () -> Unit) {
    val vm = appViewModel { JournalViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val record = state.exerciseRecords.firstOrNull { it.exerciseId == exerciseId }
    val name = state.exercises[exerciseId]?.name ?: ""
    Scaffold(topBar = { BackTopBar(name, onBack) }) { padding ->
        val r = record ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoTile("Максимальный вес", "${formatWeight(r.maxWeight)} кг", Modifier.weight(1f))
                    InfoTile("Лучший подход", "${formatWeight(r.bestWeight)} × ${r.bestReps}", Modifier.weight(1f))
                }
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Рост максимального веса")
                    Spacer(Modifier.height(8.dp))
                    LineChart(r.history, xLabel = ::formatDateShort, yLabel = { "${formatWeight(it)} кг" })
                }
            }
            items(r.history.reversed()) { (date, weight) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(formatDate(date), Modifier.weight(1f))
                    Text("${formatWeight(weight)} кг", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ComplexRecordScreen(workoutId: String, onBack: () -> Unit, onEntry: (Long) -> Unit) {
    val vm = appViewModel { JournalViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val record = state.complexRecords.firstOrNull { it.workoutId == workoutId }
    Scaffold(topBar = { BackTopBar(record?.title ?: "", onBack) }) { padding ->
        val r = record ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { InfoTile("Лучший результат", r.best.resultText() + " · " + formatDate(r.best.date), Modifier.fillMaxWidth()) }
            item { TileLabel("Все попытки") }
            items(r.attempts, key = { it.id }) { e ->
                EntryCard(e) { onEntry(e.id) }
            }
        }
    }
}

