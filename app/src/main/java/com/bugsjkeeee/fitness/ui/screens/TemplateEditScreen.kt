package com.bugsjkeeee.fitness.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.fitness.data.FitnessRepository
import com.bugsjkeeee.fitness.data.Template
import com.bugsjkeeee.fitness.data.TemplateExercise
import com.bugsjkeeee.fitness.ui.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateItem(val exerciseId: Long, val name: String, val sets: Int)

data class TemplateEditState(
    val loaded: Boolean = false,
    val name: String = "",
    val items: List<TemplateItem> = emptyList(),
)

class TemplateEditViewModel(private val repo: FitnessRepository, private val templateId: Long) : ViewModel() {
    private val _state = MutableStateFlow(TemplateEditState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val template = if (templateId != 0L) repo.template(templateId) else null
            val items = if (template != null) {
                repo.templateExercises(template.id).map { TemplateItem(it.exerciseId, it.exerciseName, it.sets) }
            } else {
                emptyList()
            }
            _state.value = TemplateEditState(loaded = true, name = template?.name ?: "", items = items)
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name) }

    fun addExercise(exerciseId: Long) = viewModelScope.launch {
        val exercise = repo.exercises.first().firstOrNull { it.id == exerciseId } ?: return@launch
        _state.update { it.copy(items = it.items + TemplateItem(exercise.id, exercise.name, 3)) }
    }

    fun changeSets(index: Int, delta: Int) = _state.update { s ->
        s.copy(items = s.items.mapIndexed { i, item ->
            if (i == index) item.copy(sets = (item.sets + delta).coerceIn(1, 10)) else item
        })
    }

    fun move(index: Int, delta: Int) = _state.update { s ->
        val target = index + delta
        if (target !in s.items.indices) return@update s
        val list = s.items.toMutableList()
        list.add(target, list.removeAt(index))
        s.copy(items = list)
    }

    fun remove(index: Int) = _state.update { s -> s.copy(items = s.items.filterIndexed { i, _ -> i != index }) }

    suspend fun save() {
        val s = _state.value
        repo.saveTemplate(
            Template(id = templateId, name = s.name.trim()),
            s.items.map { TemplateExercise(templateId = templateId, exerciseId = it.exerciseId, position = 0, sets = it.sets) },
        )
    }

    suspend fun delete() = repo.deleteTemplate(templateId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    templateId: Long,
    pickedExerciseId: Long?,
    onPickedConsumed: () -> Unit,
    onAddExercise: () -> Unit,
    onBack: () -> Unit,
) {
    val vm = appViewModel { TemplateEditViewModel(it, templateId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pickedExerciseId, state.loaded) {
        if (pickedExerciseId != null && state.loaded) {
            vm.addExercise(pickedExerciseId)
            onPickedConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId == 0L) "Новый шаблон" else "Шаблон") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (templateId != 0L) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить шаблон")
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                vm.save()
                                onBack()
                            }
                        },
                        enabled = state.name.isNotBlank() && state.items.isNotEmpty(),
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Сохранить") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = vm::setName,
                    label = { Text("Название, например «Верх» или «Ноги»") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            itemsIndexed(state.items) { index, item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { vm.changeSets(index, -1) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Меньше подходов")
                        }
                        Text("${item.sets}×", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { vm.changeSets(index, 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Больше подходов")
                        }
                        IconButton(onClick = { vm.move(index, -1) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Выше")
                        }
                        IconButton(onClick = { vm.move(index, 1) }, enabled = index < state.items.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Ниже")
                        }
                        IconButton(onClick = { vm.remove(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Убрать")
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Упражнение")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить шаблон?") },
            text = { Text("История тренировок, начатых по шаблону, сохранится.") },
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
