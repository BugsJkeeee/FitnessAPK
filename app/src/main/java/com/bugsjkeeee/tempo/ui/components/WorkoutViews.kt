package com.bugsjkeeee.tempo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.bugsjkeeee.tempo.ui.icons.TempoIcons
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
            IconBox(
                when (workout.type) {
                    WorkoutType.STRENGTH -> TempoIcons.Dumbbell
                    WorkoutType.CARDIO -> TempoIcons.Heart
                    WorkoutType.FUNCTIONAL -> TempoIcons.Flame
                },
                44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(workout.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (favorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(TempoIcons.HeartFilled, contentDescription = "В избранном", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }
                Text(workout.subtitle(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.primary)
                Text(workout.equipmentText(), style = MaterialTheme.typography.bodySmall, color = style.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(TempoIcons.ChevronRight, contentDescription = null, tint = style.muted, modifier = Modifier.size(18.dp))
        }
    }
}

/** Подробное описание тренировки: параметры, оборудование, упражнения с картинками. */
@Composable
fun WorkoutDetails(workout: Workout, exercises: Map<String, Exercise>, onExercise: (Exercise) -> Unit) {
    val style = LocalTempoStyle.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoTile("Формат", workout.format.title, Modifier.weight(1f).fillMaxHeight(), TempoIcons.Format)
            InfoTile("Время", "~${workout.durationMin} мин", Modifier.weight(1f).fillMaxHeight(), TempoIcons.Timer)
            InfoTile("Сложность", Level.of(workout.level).title, Modifier.weight(1f).fillMaxHeight(), TempoIcons.Flame)
        }
        Tile(Modifier.fillMaxWidth()) {
            TileLabel("Оборудование", TempoIcons.Dumbbell)
            Text(workout.equipmentText(), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            TileLabel("Мышцы", TempoIcons.Muscle)
            Text(workout.muscles.joinToString(", ") { it.title }, style = MaterialTheme.typography.bodyMedium)
        }
        if (workout.description.isNotBlank()) Note(workout.description, Modifier.padding(horizontal = 4.dp))
        Tile(Modifier.fillMaxWidth()) {
            TileLabel("Упражнения", TempoIcons.Clipboard)
            workout.items.forEachIndexed { i, item ->
                val ex = exercises[item.exercise]
                if (i > 0) HorizontalDivider(color = style.outline.copy(alpha = if (style.dark) 1f else 0.6f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = ex != null) { ex?.let(onExercise) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseImage(ex?.images?.firstOrNull(), Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex?.name ?: item.exercise, style = MaterialTheme.typography.titleSmall)
                        val dose = item.doseText()
                        if (dose.isNotEmpty()) Text(dose, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(TempoIcons.ChevronRight, contentDescription = null, tint = style.muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Множественный выбор чипами с обводкой; выбранные — оранжевые. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiSelector(options: List<T>, selected: Set<T>, label: (T) -> String, onToggle: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { option -> Chip(label(option), option in selected, { onToggle(option) }, outlined = true) }
    }
}

/** Карточка одного фильтра: «Любой/Любая» сбрасывает выбор, остальные чипы отмечаются по несколько. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Any> FilterCard(
    title: String,
    icon: ImageVector,
    anyLabel: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onChange: (Set<T>) -> Unit,
) {
    Tile(Modifier.fillMaxWidth()) {
        TileLabel(title, icon)
        Spacer(Modifier.height(8.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Chip(anyLabel, selected.isEmpty(), { onChange(emptySet()) })
            options.forEach { option ->
                Chip(label(option), option in selected, { onChange(if (option in selected) selected - option else selected + option) })
            }
        }
    }
}

/**
 * Фильтры рандомайзера и базы. Заголовок с числом активных фильтров и «Сбросить»,
 * по нажатию на заголовок карточки фильтров сворачиваются.
 */
@Composable
fun FilterPanel(
    filters: WorkoutFilters,
    onChange: (WorkoutFilters) -> Unit,
    showFavorites: Boolean = true,
    initiallyExpanded: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val style = LocalTempoStyle.current
    val active = listOf(filters.types, filters.muscles, filters.durations, filters.formats, filters.levels).count { it.isNotEmpty() } +
        (if (filters.equipment.isNotEmpty()) 1 else 0) + (if (filters.onlyFavorites) 1 else 0)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(TempoIcons.Filter, contentDescription = null, tint = style.secondaryText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                (if (active == 0) "Фильтры" else "Фильтры · $active").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (active > 0) {
                Text(
                    "Сбросить",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.clickable { onChange(WorkoutFilters(query = filters.query)) }.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Icon(if (expanded) TempoIcons.ChevronUp else TempoIcons.ChevronDown, contentDescription = null, tint = style.muted, modifier = Modifier.size(18.dp))
        }
        if (expanded) {
            FilterCard("Тип", TempoIcons.Format, "Любой", WorkoutType.entries, filters.types, { it.title }) { onChange(filters.copy(types = it)) }
            FilterCard("Группа мышц", TempoIcons.Muscle, "Любая", Muscle.entries, filters.muscles, { it.title }) { onChange(filters.copy(muscles = it)) }
            FilterCard("Длительность", TempoIcons.Timer, "Любая", DurationRange.entries, filters.durations, { it.title }) { onChange(filters.copy(durations = it)) }
            FilterCard("Формат", TempoIcons.Clipboard, "Любой", WorkoutFormat.entries, filters.formats, { it.title }) { onChange(filters.copy(formats = it)) }
            FilterCard("Сложность", TempoIcons.Flame, "Любая", Level.entries, filters.levels, { it.title }) { onChange(filters.copy(levels = it)) }
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Оборудование", TempoIcons.Dumbbell)
                Spacer(Modifier.height(8.dp))
                MultiSelector(
                    options = Equipment.entries.filter { it != Equipment.BODYWEIGHT },
                    selected = filters.equipment,
                    label = { it.title },
                    onToggle = { e -> onChange(filters.copy(equipment = if (e in filters.equipment) filters.equipment - e else filters.equipment + e)) },
                )
                Spacer(Modifier.height(10.dp))
                Note("Только отмеченное. Упражнения с собственным весом подходят всегда; ничего не отмечено — любое оборудование.")
            }
            if (showFavorites) {
                Tile(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBox(TempoIcons.Heart)
                        Spacer(Modifier.width(10.dp))
                        Text("Только из избранного", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Switch(
                            checked = filters.onlyFavorites,
                            onCheckedChange = { onChange(filters.copy(onlyFavorites = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = Color.White),
                        )
                    }
                }
            }
        }
    }
}
