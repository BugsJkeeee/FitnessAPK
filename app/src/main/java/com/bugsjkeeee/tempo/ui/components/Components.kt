package com.bugsjkeeee.tempo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import com.bugsjkeeee.tempo.ui.theme.Digits
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlin.math.abs

/** Карточка: белая с мягкой тенью в светлой теме, графитовая с рамкой в тёмной. */
@Composable
fun Tile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = LocalTempoStyle.current
    val border = if (style.dark) BorderStroke(1.dp, style.tileBorder) else null
    val elevation = if (style.dark) 0.dp else 1.dp
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = style.tileShape, color = style.tile, border = border, shadowElevation = elevation) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = style.tileShape, color = style.tile, border = border, shadowElevation = elevation) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

/** Подпись раздела карточки: мелкие заглавные буквы, серый цвет, иконка слева. */
@Composable
fun TileLabel(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val muted = LocalTempoStyle.current.muted
    Row(modifier.padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = muted)
    }
}

/** Круглая кнопка-иконка (настройки, избранное в шапке). */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color? = null) {
    val style = LocalTempoStyle.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = style.tile,
        border = if (style.dark) BorderStroke(1.dp, style.tileBorder) else null,
        shadowElevation = if (style.dark) 0.dp else 1.dp,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = tint ?: style.secondaryText, modifier = Modifier.size(19.dp))
        }
    }
}

/** Шапка экрана вкладки: крупный заголовок и кнопки справа. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

/** Чип выбора: активный — оранжевый, неактивный — серая подложка, «пустой» — с обводкой. */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    center: Boolean = false,
) {
    val style = LocalTempoStyle.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = style.smallShape
    val base = modifier
        .then(if (selected) Modifier.shadow(6.dp, shape, ambientColor = accent, spotColor = accent) else Modifier)
        .background(
            when {
                selected -> accent
                outlined -> if (style.dark) Color.Transparent else style.tile
                else -> style.segmentIdle
            },
            shape,
        )
        .then(if (outlined && !selected) Modifier.border(1.5.dp, style.outline, shape) else Modifier)
        .clickable(onClick = onClick)
        .defaultMinSize(minHeight = 40.dp)
        .padding(horizontal = 13.dp, vertical = 10.dp)
    Box(base, contentAlignment = if (center) Alignment.Center else Alignment.CenterStart) {
        Text(
            text,
            color = when {
                selected -> Color.White
                outlined -> style.secondaryText
                else -> style.chipText
            },
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Одиночный выбор. [flow] — чипы по ширине подписи с переносом (фильтры);
 * иначе — сетка одинаковых ячеек по [columns] в ряд (режимы, периоды).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = minOf(options.size, 3),
    flow: Boolean = false,
) {
    if (flow) {
        FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            options.forEach { Chip(label(it), it == selected, { onSelect(it) }) }
        }
        return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { option ->
                    Chip(label(option), option == selected, { onSelect(option) }, Modifier.weight(1f).fillMaxHeight(), center = true)
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Переключатель вкладок внутри экрана: серая «таблетка» с оранжевым активным сегментом. */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val style = LocalTempoStyle.current
    Row(
        modifier.fillMaxWidth().background(style.segmentIdle, style.smallShape).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, title ->
            val active = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = if (active) Color.White else style.muted)
            }
        }
    }
}

/** Главная кнопка: оранжевая, с тенью (в тёмной теме — градиент). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp,
) {
    val style = LocalTempoStyle.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = style.tileShape
    val background = when {
        !enabled -> Brush.linearGradient(listOf(style.segmentIdle, style.segmentIdle))
        style.dark -> Brush.linearGradient(listOf(Color(0xFFFF6A00), Color(0xFFFF3D00)))
        else -> Brush.linearGradient(listOf(accent, accent))
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(if (enabled) Modifier.shadow(10.dp, shape, ambientColor = accent, spotColor = accent) else Modifier)
            .background(background, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (enabled) Color.White else style.muted
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp))
    }
}

/** Второстепенная кнопка с обводкой. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color? = null,
    enabled: Boolean = true,
) {
    val style = LocalTempoStyle.current
    val tint = color ?: style.secondaryText
    Row(
        modifier
            .height(46.dp)
            .background(if (style.dark) Color.Transparent else style.tile, style.smallShape)
            .border(1.5.dp, style.outline, style.smallShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = tint, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** Плавающая кнопка «+». */
@Composable
fun AddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .size(56.dp)
            .shadow(12.dp, CircleShape, ambientColor = accent, spotColor = accent)
            .background(accent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(TempoIcons.Add, contentDescription = "Добавить", tint = Color.White, modifier = Modifier.size(24.dp)) }
}

/** Квадратная серая подложка под иконку (строки параметров, упражнения). */
@Composable
fun IconBox(icon: ImageVector, size: Dp = 36.dp) {
    val style = LocalTempoStyle.current
    Box(Modifier.size(size).background(style.segmentIdle, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = style.secondaryText, modifier = Modifier.size(size * 0.45f))
    }
}

/** Строка параметра: иконка, подпись с пояснением и элемент управления справа. */
@Composable
fun ParamRow(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, sub: String? = null, trailing: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            IconBox(icon)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = LocalTempoStyle.current.muted)
        }
        trailing()
    }
}

/** «Таблетка» −/значение/+; нажатие на значение открывает барабан. */
@Composable
fun StepperPill(valueText: String, onMinus: () -> Unit, onPlus: () -> Unit, onEdit: () -> Unit) {
    val style = LocalTempoStyle.current
    Row(
        Modifier.background(style.segmentIdle, style.smallShape).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
            Icon(TempoIcons.Minus, contentDescription = "Меньше", tint = style.secondaryText, modifier = Modifier.size(16.dp))
        }
        Text(
            valueText,
            modifier = Modifier.width(64.dp).clickable(onClick = onEdit).padding(vertical = 6.dp),
            textAlign = TextAlign.Center,
            style = Digits.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(Modifier.size(32.dp).clickable(onClick = onPlus), contentAlignment = Alignment.Center) {
            Icon(TempoIcons.Add, contentDescription = "Больше", tint = style.secondaryText, modifier = Modifier.size(16.dp))
        }
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
    icon: ImageVector? = TempoIcons.Timer,
    sub: String? = null,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    ParamRow(label, modifier, icon, sub) {
        StepperPill(
            valueText = if (seconds == 0 && zeroText != null) zeroText else formatClock(seconds * 1000L),
            onMinus = { onChange((seconds - durationStep(seconds, up = false)).coerceIn(min, max)) },
            onPlus = { onChange((seconds + durationStep(seconds, up = true)).coerceIn(min, max)) },
            onEdit = { editing = true },
        )
    }
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
    icon: ImageVector? = TempoIcons.Refresh,
    sub: String? = null,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    ParamRow(label, modifier, icon, sub) {
        StepperPill(
            valueText = value.toString(),
            onMinus = { onChange((value - 1).coerceIn(min, max)) },
            onPlus = { onChange((value + 1).coerceIn(min, max)) },
            onEdit = { editing = true },
        )
    }
    if (editing) {
        NumberWheelDialog(label, value, min, max, onDismiss = { editing = false }) {
            editing = false
            onChange(it)
        }
    }
}

/** Выбор числа одним барабаном (раунды, повторы, секунды подготовки). */
@Composable
fun NumberWheelDialog(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    label: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var current by rememberSaveable { mutableIntStateOf(value.coerceIn(min, max)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WheelPicker(count = max - min + 1, selected = current - min, onSelected = { current = it + min }, label = label, format = { "${it + min}" })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text("Готово") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun DurationDialog(title: String, seconds: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var min by rememberSaveable { mutableIntStateOf((seconds / 60).coerceAtMost(MAX_MINUTES)) }
    var sec by rememberSaveable { mutableIntStateOf(seconds % 60) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                WheelPicker(count = MAX_MINUTES + 1, selected = min, onSelected = { min = it }, label = "мин")
                Text(":", style = Digits.copy(fontSize = 32.sp), modifier = Modifier.padding(horizontal = 8.dp))
                WheelPicker(count = 60, selected = sec, onSelected = { sec = it }, label = "сек")
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(min * 60 + sec) }) { Text("Готово") } },
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
    format: (Int) -> String = { "%02d".format(it) },
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .background(style.segmentIdle, style.smallShape),
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
                                rotationX = (distance * 18f).coerceIn(-60f, 60f)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            format(i),
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

/** Небольшая плитка «иконка, подпись, значение» по центру. */
@Composable
fun InfoTile(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val style = LocalTempoStyle.current
    Tile(modifier, onClick = onClick, padding = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = style.secondaryText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.height(5.dp))
            }
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = style.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** Плитка счётчика: крупное число и подпись. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Tile(modifier, padding = 14.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = Digits.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium), color = LocalTempoStyle.current.muted, textAlign = TextAlign.Center)
        }
    }
}

/** Мелкий серый поясняющий текст. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = LocalTempoStyle.current.muted, modifier = modifier)
}

/** Поле ввода в стиле карточек: скругление 12 dp, без подчёркивания. */
@Composable
fun TempoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val style = LocalTempoStyle.current
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = style.muted) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = style.muted, modifier = Modifier.size(18.dp)) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle,
        shape = style.smallShape,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = style.tile,
            unfocusedContainerColor = style.tile,
            unfocusedBorderColor = style.outline,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** Главная кнопка в стиле макета с произвольным содержимым (иконка + текст). */
@Composable
fun TButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") shape: androidx.compose.ui.graphics.Shape? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val style = LocalTempoStyle.current
    val accent = MaterialTheme.colorScheme.primary
    val s = style.tileShape
    Row(
        modifier
            .defaultMinSize(minHeight = 52.dp)
            .then(if (enabled) Modifier.shadow(8.dp, s, ambientColor = accent, spotColor = accent) else Modifier)
            .background(if (enabled) accent else style.segmentIdle, s)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (enabled) Color.White else style.muted,
            androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.titleMedium,
        ) { content() }
    }
}

/** Второстепенная кнопка с обводкой и произвольным содержимым. */
@Composable
fun TOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val style = LocalTempoStyle.current
    Row(
        modifier
            .defaultMinSize(minHeight = 46.dp)
            .background(if (style.dark) Color.Transparent else style.tile, style.smallShape)
            .border(1.5.dp, style.outline, style.smallShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides style.secondaryText,
            androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        ) { content() }
    }
}

/** Поле ввода с параметрами OutlinedTextField, оформленное как карточки. */
@Composable
fun TOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val style = LocalTempoStyle.current
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle,
        shape = style.smallShape,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = style.tile,
            unfocusedContainerColor = style.tile,
            unfocusedBorderColor = style.outline,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}
