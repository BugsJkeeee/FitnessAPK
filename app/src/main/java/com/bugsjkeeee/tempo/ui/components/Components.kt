package com.bugsjkeeee.tempo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugsjkeeee.tempo.ui.theme.Digits
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle

/** Плитка — основной строительный блок экранов. */
@Composable
fun Tile(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val style = LocalTempoStyle.current
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = style.tileShape,
        color = style.tile,
        border = BorderStroke(1.dp, style.tileBorder),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** Мелкая подпись заглавными буквами, при необходимости с иконкой. */
@Composable
fun TileLabel(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val muted = LocalTempoStyle.current.muted
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = muted)
    }
}

/**
 * Сегментные кнопки выбора: сетка ячеек одинаковой ширины, активная выделена акцентным цветом.
 * По умолчанию не больше трёх ячеек в ряд, чтобы подписи помещались целиком.
 */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = minOf(options.size, 3),
) {
    val style = LocalTempoStyle.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .defaultMinSize(minHeight = 48.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else style.segmentIdle,
                                style.tileShape,
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label(option),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            // 13sp: «Секундомер» целиком помещается в треть ширины узкого экрана.
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
                // Неполный последний ряд добивается пустыми ячейками, чтобы ширина совпадала с верхними.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Значение с кнопками −/+ и вводом вручную по нажатию. */
@Composable
private fun Stepper(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = LocalTempoStyle.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, style.tileBorder, style.tileShape)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = style.muted,
        )
        IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, contentDescription = "Меньше") }
        Text(
            valueText,
            modifier = Modifier.width(92.dp).clickable(onClick = onEdit).padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = Digits.copy(fontSize = 22.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, contentDescription = "Больше") }
    }
}

/** Шаг изменения длительности зависит от величины: секунды — по 5, минуты — по 15 и 60. */
private fun durationStep(sec: Int, up: Boolean): Int {
    val ref = if (up) sec else sec - 1
    return when {
        ref < 60 -> 5
        ref < 5 * 60 -> 15
        else -> 60
    }
}

@Composable
fun DurationStepper(
    label: String,
    seconds: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 5,
    max: Int = 3 * 60 * 60,
    zeroText: String? = null,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    Stepper(
        label = label,
        valueText = if (seconds == 0 && zeroText != null) zeroText else formatClock(seconds * 1000L),
        onMinus = { onChange((seconds - durationStep(seconds, up = false)).coerceIn(min, max)) },
        onPlus = { onChange((seconds + durationStep(seconds, up = true)).coerceIn(min, max)) },
        onEdit = { editing = true },
        modifier = modifier,
    )
    if (editing) {
        DurationDialog(label, seconds, onDismiss = { editing = false }) {
            editing = false
            onChange(it.coerceIn(min, max))
        }
    }
}

@Composable
fun CountStepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 99,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    Stepper(
        label = label,
        valueText = value.toString(),
        onMinus = { onChange((value - 1).coerceIn(min, max)) },
        onPlus = { onChange((value + 1).coerceIn(min, max)) },
        onEdit = { editing = true },
        modifier = modifier,
    )
    if (editing) {
        var text by rememberSaveable { mutableStateOf(value.toString()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editing = false
                    text.toIntOrNull()?.let { onChange(it.coerceIn(min, max)) }
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun DurationDialog(title: String, seconds: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var min by rememberSaveable { mutableIntStateOf((seconds / 60).coerceAtMost(MAX_MINUTES)) }
    var sec by rememberSaveable { mutableIntStateOf(seconds % 60) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(count = MAX_MINUTES + 1, selected = min, onSelected = { min = it }, label = "мин")
                Text(":", style = Digits.copy(fontSize = 32.sp), modifier = Modifier.padding(horizontal = 8.dp))
                WheelPicker(count = 60, selected = sec, onSelected = { sec = it }, label = "сек")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(min * 60 + sec) }) { Text("Готово") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

private const val MAX_MINUTES = 180
private val WheelItemHeight = 52.dp
private const val WheelVisibleItems = 5

/**
 * Барабан выбора числа: прокрутка вверх-вниз с доводкой к центру,
 * числа по краям уменьшаются и бледнеют.
 */
@Composable
fun WheelPicker(
    count: Int,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(0, count - 1))
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val accent = MaterialTheme.colorScheme.primary
    val style = LocalTempoStyle.current

    // Выбранным считается элемент, ближайший к центру барабана.
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index ?: selected
        }
    }
    LaunchedEffect(centered) { onSelected(centered) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(84.dp).height(WheelItemHeight * WheelVisibleItems), contentAlignment = Alignment.Center) {
            // Рамка выбранного значения.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .border(1.5.dp, accent.copy(alpha = 0.6f), style.tileShape),
            )
            LazyColumn(
                state = state,
                flingBehavior = fling,
                contentPadding = PaddingValues(vertical = WheelItemHeight * (WheelVisibleItems / 2)),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count) { i ->
                    Box(
                        Modifier
                            .height(WheelItemHeight)
                            .fillMaxWidth()
                            .graphicsLayer {
                                val info = state.layoutInfo
                                val item = info.visibleItemsInfo.firstOrNull { it.index == i } ?: return@graphicsLayer
                                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                val distance = (item.offset + item.size / 2f - center) / item.size
                                val d = abs(distance)
                                alpha = (1f - 0.32f * d).coerceIn(0.15f, 1f)
                                scaleX = (1f - 0.12f * d).coerceIn(0.7f, 1f)
                                scaleY = scaleX
                                // Лёгкий наклон, как у настоящего барабана.
                                rotationX = (distance * 18f).coerceIn(-60f, 60f)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "%02d".format(i),
                            style = Digits.copy(fontSize = 30.sp),
                            color = if (i == centered) accent else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = style.muted)
        }
    }
}

/** Заглушка раздела, который появится на следующих этапах. */
@Composable
fun ComingSoon(title: String, stage: Int, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        TileLabel("Этап $stage")
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = LocalTempoStyle.current.muted)
    }
}
