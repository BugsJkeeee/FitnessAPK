package com.bugsjkeeee.fitness.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.fitness.data.BodyWeight
import com.bugsjkeeee.fitness.data.FitnessRepository
import com.bugsjkeeee.fitness.data.formatWeight
import com.bugsjkeeee.fitness.ui.TabScreenInsets
import com.bugsjkeeee.fitness.ui.appViewModel
import com.bugsjkeeee.fitness.ui.components.ChartPoint
import com.bugsjkeeee.fitness.ui.components.LineChart
import com.bugsjkeeee.fitness.ui.components.SectionTitle
import com.bugsjkeeee.fitness.ui.components.formatDateTime
import com.bugsjkeeee.fitness.ui.components.parseDecimal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BodyWeightViewModel(private val repo: FitnessRepository) : ViewModel() {
    val entries: StateFlow<List<BodyWeight>> =
        repo.bodyWeights.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(weight: Double) = viewModelScope.launch { repo.addBodyWeight(weight) }
    fun delete(entry: BodyWeight) = viewModelScope.launch { repo.deleteBodyWeight(entry) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightScreen() {
    val vm = appViewModel { BodyWeightViewModel(it) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val parsed = parseDecimal(input)?.takeIf { it in 20.0..400.0 }
    val submit = {
        parsed?.let {
            vm.add(it)
            input = ""
            focus.clearFocus()
        }
    }

    Scaffold(
        contentWindowInsets = TabScreenInsets,
        topBar = { TopAppBar(title = { Text("Вес тела") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Вес, кг") },
                        placeholder = { Text(entries.firstOrNull()?.let { formatWeight(it.weight) } ?: "75.5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { submit() }, enabled = parsed != null, modifier = Modifier.height(56.dp)) {
                        Text("Добавить")
                    }
                }
            }
            if (entries.size >= 2) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(16.dp)) {
                            val change = entries.first().weight - entries.last().weight
                            Text(
                                "Изменение: ${if (change > 0) "+" else ""}${formatWeight(change)} кг",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            LineChart(
                                entries.map { ChartPoint(it.date, it.weight) },
                                lineColor = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
            if (entries.isNotEmpty()) item { SectionTitle("Замеры") }
            items(entries, key = { it.id }) { entry ->
                ListItem(
                    headlineContent = { Text("${formatWeight(entry.weight)} кг", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text(formatDateTime(entry.date)) },
                    trailingContent = {
                        IconButton(onClick = { vm.delete(entry) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить замер")
                        }
                    },
                )
            }
        }
    }
}
