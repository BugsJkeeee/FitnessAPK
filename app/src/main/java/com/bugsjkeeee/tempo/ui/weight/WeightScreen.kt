package com.bugsjkeeee.tempo.ui.weight

import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import com.bugsjkeeee.tempo.ui.components.TButton
import com.bugsjkeeee.tempo.ui.components.TOutlinedButton
import com.bugsjkeeee.tempo.ui.components.TOutlinedTextField
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.data.WeightPoint
import com.bugsjkeeee.tempo.data.weightTrend
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.InfoTile
import com.bugsjkeeee.tempo.ui.components.LineChart
import com.bugsjkeeee.tempo.ui.components.SegmentedSelector
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.formatEpochDay
import com.bugsjkeeee.tempo.ui.components.formatWeight
import com.bugsjkeeee.tempo.ui.components.parseDecimal
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WeightPeriod(val title: String, val days: Long?) {
    MONTH("Месяц", 30), QUARTER("3 месяца", 91), YEAR("Год", 365), ALL("Всё время", null),
}

data class WeightState(
    val loaded: Boolean = false,
    val points: List<WeightPoint> = emptyList(),
    val target: Double? = null,
)

class WeightViewModel(private val app: TempoApp) : ViewModel() {
    val state: StateFlow<WeightState> = combine(app.weightRepository.weights, app.settingsRepository.settings) { w, s ->
        WeightState(true, w, s.targetWeight)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightState())

    fun save(day: Long, weight: Double) {
        viewModelScope.launch { app.weightRepository.save(day, weight) }
    }

    fun delete(day: Long) {
        viewModelScope.launch { app.weightRepository.delete(day) }
    }
}

private val shortDay = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { WeightViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val style = LocalTempoStyle.current
    val focus = LocalFocusManager.current
    var input by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var period by rememberSaveable { mutableStateOf(WeightPeriod.QUARTER) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    if (!state.loaded) return

    val parsed = parseDecimal(input)?.takeIf { it in 20.0..400.0 }
    val today = LocalDate.now().toEpochDay()
    val inPeriod = state.points
        .filter { p -> period.days?.let { p.epochDay >= today - it } ?: true }
        .sortedBy { it.epochDay }
    val current = state.points.maxByOrNull { it.epochDay }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Новый замер")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TOutlinedTextField(
                        value = input,
                        onValueChange = { input = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                        label = { Text("Вес, кг") },
                        placeholder = { Text(current?.let { formatWeight(it.weight) } ?: "75,5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    TOutlinedButton(onClick = { showDate = true }, modifier = Modifier.height(56.dp)) {
                        Icon(TempoIcons.Calendar, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (day == today) "Сегодня" else LocalDate.ofEpochDay(day).format(shortDay))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TButton(
                    onClick = {
                        parsed?.let { vm.save(day, Math.round(it * 10) / 10.0) }
                        input = ""
                        day = today
                        focus.clearFocus()
                    },
                    enabled = parsed != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = style.tileShape,
                ) { Text("Сохранить") }
                if (state.points.any { it.epochDay == day }) {
                    Text("На эту дату уже есть запись — она будет заменена.", style = MaterialTheme.typography.bodySmall, color = style.muted)
                }
            }
        }

        if (current != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoTile("Текущий", "${formatWeight(current.weight)} кг", Modifier.weight(1f))
                    val change = if (inPeriod.size >= 2) inPeriod.last().weight - inPeriod.first().weight else null
                    InfoTile(
                        "За период",
                        change?.let { (if (it > 0) "+" else if (it < 0) "−" else "") + formatWeight(kotlin.math.abs(it)) + " кг" } ?: "—",
                        Modifier.weight(1f),
                    )
                    state.target?.let { target ->
                        val left = current.weight - target
                        InfoTile(
                            "До цели",
                            if (kotlin.math.abs(left) < 0.05) "достигнута" else formatWeight(kotlin.math.abs(left)) + " кг",
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    SegmentedSelector(WeightPeriod.entries, period, { it.title }, { period = it }, columns = 4)
                    Spacer(Modifier.height(12.dp))
                    if (inPeriod.isEmpty()) {
                        Text("За выбранный период замеров нет.", color = style.muted)
                    } else {
                        val points = inPeriod.map { it.epochDay to it.weight }
                        LineChart(
                            points = points,
                            trend = weightTrend(points),
                            goal = state.target,
                            xLabel = { LocalDate.ofEpochDay(it).format(shortDay) },
                            yLabel = { "${formatWeight(it)} кг" },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Точки — замеры, тёмная линия — тренд (среднее по 7 замерам)" + if (state.target != null) ", пунктир — цель." else ". Цель задаётся в настройках.",
                            style = MaterialTheme.typography.bodySmall,
                            color = style.muted,
                        )
                    }
                }
            }
            item { TileLabel("Все замеры") }
        } else {
            item {
                Text("Введите первый замер — после двух появится график.", color = style.muted)
            }
        }
        items(state.points, key = { it.epochDay }) { p ->
            Tile(Modifier.fillMaxWidth(), onClick = { editing = p.epochDay }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatEpochDay(p.epochDay), modifier = Modifier.weight(1f))
                    Text("${formatWeight(p.weight)} кг", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    showDate = false
                    pickerState.selectedDateMillis?.let { day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Отмена") } },
        ) { DatePicker(pickerState) }
    }

    editing?.let { editDay ->
        val point = state.points.firstOrNull { it.epochDay == editDay }
        var text by rememberSaveable(editDay) { mutableStateOf(point?.weight?.let(::formatWeight).orEmpty()) }
        val value = parseDecimal(text)?.takeIf { it in 20.0..400.0 }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(formatEpochDay(editDay)) },
            text = {
                TOutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Вес, кг") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(enabled = value != null, onClick = {
                    value?.let { vm.save(editDay, Math.round(it * 10) / 10.0) }
                    editing = null
                }) { Text("Сохранить") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.delete(editDay); editing = null }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { editing = null }) { Text("Отмена") }
                }
            },
        )
    }
}
