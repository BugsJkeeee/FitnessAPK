package com.bugsjkeeee.tempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugsjkeeee.tempo.content.DurationRange
import com.bugsjkeeee.tempo.content.Equipment
import com.bugsjkeeee.tempo.content.Exercise
import com.bugsjkeeee.tempo.content.Level
import com.bugsjkeeee.tempo.content.Muscle
import com.bugsjkeeee.tempo.content.Workout
import com.bugsjkeeee.tempo.content.WorkoutFilters
import com.bugsjkeeee.tempo.content.WorkoutFormat
import com.bugsjkeeee.tempo.content.WorkoutItem
import com.bugsjkeeee.tempo.content.WorkoutType
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle

fun Workout.subtitle(): String = "${format.title} · ~$durationMin мин · ${Level.of(level).title.lowercase()}"

fun Workout.equipmentText(): String =
    equipment.joinToString(", ") { it.title }.ifEmpty { "Без оборудования" }

fun WorkoutItem.doseText(): String = when {
    sets != null && reps != null -> "$sets × $reps"
    !dose.isNullOrBlank() -> dose
    else -> ""
}

/** Карточка тренировки для списков. */
@Composable
fun WorkoutCard(workout: Workout, favorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = LocalTempoStyle.current
    Tile(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                workout.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (favorite) Icon(Icons.Filled.Favorite, contentDescription = "В избранном", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Text(workout.subtitle(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Text(workout.equipmentText(), style = MaterialTheme.typography.bodySmall, color = style.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Плитка «подпись — значение». */
@Composable
fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Tile(modifier) {
        TileLabel(label)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 2)
    }
}

/** Подробное описание тренировки: параметры, оборудование, упражнения с картинками. */
@Composable
fun WorkoutDetails(workout: Workout, exercises: Map<String, Exercise>, onExercise: (Exercise) -> Unit) {
    val style = LocalTempoStyle.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoTile("Формат", workout.format.title, Modifier.weight(1f).fillMaxHeight())
            InfoTile("Время", "~${workout.durationMin} мин", Modifier.weight(1f).fillMaxHeight())
            InfoTile("Сложность", Level.of(workout.level).title, Modifier.weight(1f).fillMaxHeight())
        }
        Tile(Modifier.fillMaxWidth()) {
            TileLabel("Оборудование")
            Text(workout.equipmentText(), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            TileLabel("Мышцы")
            Text(workout.muscles.joinToString(", ") { it.title }, style = MaterialTheme.typography.bodyLarge)
        }
        if (workout.description.isNotBlank()) {
            Text(workout.description, style = MaterialTheme.typography.bodyMedium, color = style.muted)
        }
        Tile(Modifier.fillMaxWidth()) {
            TileLabel("Упражнения")
            Spacer(Modifier.height(4.dp))
            workout.items.forEach { item ->
                val ex = exercises[item.exercise]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = ex != null) { ex?.let(onExercise) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseImage(ex?.images?.firstOrNull(), Modifier.size(56.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex?.name ?: item.exercise, style = MaterialTheme.typography.bodyLarge)
                        val dose = item.doseText()
                        if (dose.isNotEmpty()) Text(dose, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/** Множественный выбор в сетке одинаковых ячеек. */
@Composable
fun <T> MultiSelector(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
    columns: Int = 2,
) {
    val style = LocalTempoStyle.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { option ->
                    val on = option in selected
                    Text(
                        label(option),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (on) MaterialTheme.colorScheme.primary else style.segmentIdle, style.tileShape)
                            .clickable { onToggle(option) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        textAlign = TextAlign.Center,
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Одиночный выбор с вариантом «Любой». */
@Composable
private fun <T : Any> OptionalSelector(title: String, options: List<T>, selected: T?, label: (T) -> String, onSelect: (T?) -> Unit, columns: Int = 3) {
    TileLabel(title)
    Spacer(Modifier.height(6.dp))
    SegmentedSelector(
        options = listOf<T?>(null) + options,
        selected = selected,
        label = { it?.let(label) ?: "Любой" },
        onSelect = onSelect,
        columns = columns,
    )
    Spacer(Modifier.height(12.dp))
}

/** Панель фильтров рандомайзера и базы; сворачивается, в заголовке — число активных фильтров. */
@Composable
fun FilterPanel(filters: WorkoutFilters, onChange: (WorkoutFilters) -> Unit, showFavorites: Boolean = true) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val active = listOfNotNull(filters.type, filters.muscle, filters.duration, filters.format, filters.level).size +
        (if (filters.equipment.isNotEmpty()) 1 else 0) + (if (filters.onlyFavorites) 1 else 0)
    Tile(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                if (active == 0) "Фильтры" else "Фильтры · $active",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (active > 0) {
                TextButton(onClick = { onChange(WorkoutFilters(query = filters.query)) }) { Text("Сбросить") }
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            OptionalSelector("Тип", WorkoutType.entries, filters.type, { it.title }, { onChange(filters.copy(type = it)) })
            OptionalSelector("Группа мышц", Muscle.entries, filters.muscle, { it.title }, { onChange(filters.copy(muscle = it)) }, columns = 4)
            OptionalSelector("Длительность", DurationRange.entries, filters.duration, { it.title }, { onChange(filters.copy(duration = it)) }, columns = 3)
            OptionalSelector("Формат", WorkoutFormat.entries, filters.format, { it.title }, { onChange(filters.copy(format = it)) })
            OptionalSelector("Сложность", Level.entries, filters.level, { it.title }, { onChange(filters.copy(level = it)) }, columns = 4)
            TileLabel("Оборудование — только отмеченное")
            Spacer(Modifier.height(6.dp))
            MultiSelector(
                options = Equipment.entries.filter { it != Equipment.BODYWEIGHT },
                selected = filters.equipment,
                label = { it.title },
                onToggle = { e -> onChange(filters.copy(equipment = if (e in filters.equipment) filters.equipment - e else filters.equipment + e)) },
            )
            Text(
                "Упражнения с собственным весом подходят всегда. Ничего не отмечено — любое оборудование.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalTempoStyle.current.muted,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (showFavorites) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Только из избранного", modifier = Modifier.weight(1f))
                    Switch(checked = filters.onlyFavorites, onCheckedChange = { onChange(filters.copy(onlyFavorites = it)) })
                }
            }
        }
    }
}
