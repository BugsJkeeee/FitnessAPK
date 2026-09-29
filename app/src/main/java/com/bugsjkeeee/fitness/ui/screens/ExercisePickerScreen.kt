package com.bugsjkeeee.fitness.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugsjkeeee.fitness.data.Exercise
import com.bugsjkeeee.fitness.data.FitnessRepository
import com.bugsjkeeee.fitness.ui.appViewModel
import com.bugsjkeeee.fitness.ui.components.SectionTitle
import kotlinx.coroutines.launch

private val muscleGroups = listOf("Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс", "Другое")

class ExercisePickerViewModel(private val repo: FitnessRepository) : ViewModel() {
    val exercises = repo.exercises

    suspend fun addCustom(name: String, group: String): Long? = repo.addCustomExercise(name, group)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    pickMode: Boolean,
    onPicked: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val vm = appViewModel { ExercisePickerViewModel(it) }
    val exercises by vm.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(exercises, query) {
        exercises.filter { it.name.contains(query.trim(), ignoreCase = true) }.groupBy { it.muscleGroup }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pickMode) "Выбор упражнения" else "Упражнения") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Своё упражнение")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
            LazyColumn(Modifier.fillMaxSize()) {
                filtered.forEach { (group, list) ->
                    item(key = "header_$group") { SectionTitle(group, Modifier.padding(horizontal = 16.dp)) }
                    items(list, key = { it.id }) { exercise ->
                        ExerciseRow(exercise, enabled = pickMode) { onPicked(exercise.id) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            initialName = query,
            onDismiss = { showAddDialog = false },
            onAdd = vm::addCustom,
            onAdded = { id ->
                showAddDialog = false
                if (pickMode) onPicked(id)
            },
        )
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, enabled: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(exercise.name) },
        supportingContent = if (exercise.isCustom) {
            { Text("своё", color = MaterialTheme.colorScheme.secondary) }
        } else {
            null
        },
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExerciseDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onAdd: suspend (String, String) -> Long?,
    onAdded: (Long) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var group by rememberSaveable { mutableStateOf(muscleGroups.last()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое упражнение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Название") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    muscleGroups.forEach { g ->
                        FilterChip(selected = group == g, onClick = { group = g }, label = { Text(g) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    scope.launch {
                        val id = onAdd(name, group)
                        if (id == null) error = "Такое упражнение уже есть" else onAdded(id)
                    }
                },
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
