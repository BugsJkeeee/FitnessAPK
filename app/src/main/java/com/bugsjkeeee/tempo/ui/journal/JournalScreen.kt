package com.bugsjkeeee.tempo.ui.journal

import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import com.bugsjkeeee.tempo.ui.components.RoundIconButton
import com.bugsjkeeee.tempo.ui.components.SegmentedControl
import com.bugsjkeeee.tempo.ui.components.AddFab
import com.bugsjkeeee.tempo.ui.components.StatTile
import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.content.Exercise
import com.bugsjkeeee.tempo.content.WorkoutFormat
import com.bugsjkeeee.tempo.data.ComplexRecord
import com.bugsjkeeee.tempo.data.ExerciseRecord
import com.bugsjkeeee.tempo.data.JournalEntry
import com.bugsjkeeee.tempo.data.Regularity
import com.bugsjkeeee.tempo.data.complexRecords
import com.bugsjkeeee.tempo.data.exerciseRecords
import com.bugsjkeeee.tempo.data.regularity
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.InfoTile
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.formatClock
import com.bugsjkeeee.tempo.ui.components.formatDate
import com.bugsjkeeee.tempo.ui.components.formatPrecise
import com.bugsjkeeee.tempo.ui.components.formatWeight
import com.bugsjkeeee.tempo.ui.components.plural
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class JournalState(
    val loaded: Boolean = false,
    val entries: List<JournalEntry> = emptyList(),
    val exercises: Map<String, Exercise> = emptyMap(),
    val regularity: Regularity = Regularity(0, 0, 0),
    val exerciseRecords: List<ExerciseRecord> = emptyList(),
    val complexRecords: List<ComplexRecord> = emptyList(),
)

class JournalViewModel(private val app: TempoApp) : ViewModel() {
    fun delete(id: Long) {
        viewModelScope.launch { app.journalRepository.delete(id) }
    }

    val state: StateFlow<JournalState> = combine(app.journalRepository.entries, app.contentRepository.exercisesFlow) { entries, ex ->
        val map = ex.associateBy { it.id }
        JournalState(
            loaded = true,
            entries = entries,
            exercises = map,
            regularity = regularity(entries.map { it.date }, LocalDate.now()),
            exerciseRecords = exerciseRecords(entries).sortedBy { map[it.exerciseId]?.name ?: it.exerciseId },
            complexRecords = complexRecords(entries),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalState())
}

/** Краткий результат записи для списков. */
fun JournalEntry.resultText(): String = when {
    format == WorkoutFormat.FOR_TIME && resultTimeMs != null -> "Время ${formatPrecise(resultTimeMs)}"
    format == WorkoutFormat.AMRAP && resultRounds != null -> plural(resultRounds, "раунд", "раунда", "раундов")
    volume > 0 -> "Тоннаж ${formatWeight(volume)} кг"
    sets.isNotEmpty() -> plural(sets.size, "подход", "подхода", "подходов")
    else -> ""
}

private val journalTabs = listOf("Список", "Календарь", "Рекорды")

@Composable
fun JournalScreen(
    contentPadding: PaddingValues,
    onEntry: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    onExerciseRecord: (String) -> Unit,
    onComplexRecord: (String) -> Unit,
) {
    val vm = appViewModel { JournalViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var toDelete by remember { mutableStateOf<JournalEntry?>(null) }
    if (!state.loaded) return

    Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Column(Modifier.fillMaxSize()) {
            SegmentedControl(journalTabs, tab, { tab = it }, Modifier.padding(horizontal = 16.dp))
            val padding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = contentPadding.calculateBottomPadding() + 88.dp)
            when (tab) {
                0 -> EntryList(state.entries, padding, onEntry, onEdit) { toDelete = it }
                1 -> CalendarTab(state, padding, onEntry)
                else -> RecordsTab(state, padding, onExerciseRecord, onComplexRecord)
            }
        }
        AddFab(onAdd, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp))
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить запись?") },
            text = { Text("«${e.title}» от ${formatDate(e.date)} будет удалена из журнала.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(e.id)
                    toDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Отмена") } },
        )
    }
}

/** Свайп влево открывает справа кнопки «Редактировать» и «Удалить». */
@Composable
private fun SwipeActions(onEdit: () -> Unit, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val revealPx = with(LocalDensity.current) { 104.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    fun close() = scope.launch { offset.animateTo(0f) }
    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Row(
            Modifier.matchParentSize().padding(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(TempoIcons.Edit, "Редактировать", { close(); onEdit() })
            RoundIconButton(TempoIcons.Trash, "Удалить", { close(); onDelete() }, tint = MaterialTheme.colorScheme.error)
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-revealPx, 0f)) }
                    },
                    onDragStopped = { velocity ->
                        val open = offset.value < -revealPx / 2 || velocity < -800f
                        offset.animateTo(if (open && velocity <= 800f) -revealPx else 0f)
                    },
                ),
        ) { content() }
    }
}

@Composable
fun EntryCard(entry: JournalEntry, onClick: () -> Unit) {
    val style = LocalTempoStyle.current
    Tile(Modifier.fillMaxWidth(), onClick = onClick) {
        Text(entry.title, style = MaterialTheme.typography.titleMedium)
        Text(
            listOfNotNull(formatDate(entry.date), entry.format?.title, entry.durationMs?.let { formatClock(it) }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = style.muted,
        )
        val result = entry.resultText()
        if (result.isNotEmpty()) Text(result, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EntryList(
    entries: List<JournalEntry>,
    padding: PaddingValues,
    onEntry: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (entries.isEmpty()) {
            item {
                Text(
                    "Здесь появятся выполненные тренировки: силовые сохраняются сами, результаты таймера — кнопкой «Записать в журнал», остальное можно добавить вручную кнопкой «+».",
                    color = LocalTempoStyle.current.muted,
                )
            }
        }
        items(entries, key = { it.id }) { e ->
            SwipeActions(onEdit = { onEdit(e.id) }, onDelete = { onDelete(e) }) { EntryCard(e) { onEntry(e.id) } }
        }
    }
}

@Composable
private fun CalendarTab(state: JournalState, padding: PaddingValues, onEntry: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val month = YearMonth.now().plusMonths(monthOffset.toLong())
    val byDay = state.entries.groupBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
    val style = LocalTempoStyle.current
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${state.regularity.week}", "за неделю", Modifier.weight(1f))
                StatTile("${state.regularity.month}", "за месяц", Modifier.weight(1f))
                StatTile("${state.regularity.streakWeeks}", "недель подряд", Modifier.weight(1f))
            }
        }
        item {
            Tile(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(TempoIcons.ChevronLeft, "Предыдущий месяц", { monthOffset-- })
                    Text(
                        month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru")).replaceFirstChar { it.uppercase() } + " " + month.year,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    RoundIconButton(TempoIcons.ChevronRight, "Следующий месяц", { monthOffset++ })
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = style.muted)
                    }
                }
                Spacer(Modifier.height(4.dp))
                val first = month.atDay(1)
                val shift = first.dayOfWeek.value - 1
                val cells = List(shift) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (day != null) {
                                    val has = byDay.containsKey(day)
                                    val selected = day.toEpochDay() == selectedDay
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                when {
                                                    has -> accent
                                                    selected -> style.segmentIdle
                                                    else -> androidx.compose.ui.graphics.Color.Transparent
                                                },
                                                style.tileShape,
                                            )
                                            .clickable { selectedDay = day.toEpochDay() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "${day.dayOfMonth}",
                                            color = if (has) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        val dayEntries = byDay[LocalDate.ofEpochDay(selectedDay)].orEmpty()
        item { TileLabel(formatDate(LocalDate.ofEpochDay(selectedDay).atStartOfDay(zone).toInstant().toEpochMilli())) }
        if (dayEntries.isEmpty()) item { Text("Тренировок нет", color = style.muted) }
        items(dayEntries, key = { it.id }) { EntryCard(it) { onEntry(it.id) } }
    }
}

@Composable
private fun RecordsTab(state: JournalState, padding: PaddingValues, onExercise: (String) -> Unit, onComplex: (String) -> Unit) {
    val style = LocalTempoStyle.current
    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { TileLabel("Комплексы") }
        if (state.complexRecords.isEmpty()) {
            item { Text("Появятся, когда вы запишете результат одного комплекса хотя бы дважды.", color = style.muted) }
        }
        items(state.complexRecords, key = { "c_" + it.workoutId }) { r ->
            Tile(Modifier.fillMaxWidth(), onClick = { onComplex(r.workoutId) }) {
                Text(r.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Лучший: ${r.best.resultText()} · попыток: ${r.attempts.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)); TileLabel("Упражнения") }
        if (state.exerciseRecords.isEmpty()) {
            item { Text("Появятся после первых подходов с весом.", color = style.muted) }
        }
        items(state.exerciseRecords, key = { "e_" + it.exerciseId }) { r ->
            Tile(Modifier.fillMaxWidth(), onClick = { onExercise(r.exerciseId) }) {
                Text(state.exercises[r.exerciseId]?.name ?: r.exerciseId, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Максимум ${formatWeight(r.maxWeight)} кг · лучший подход ${formatWeight(r.bestWeight)} × ${r.bestReps}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
