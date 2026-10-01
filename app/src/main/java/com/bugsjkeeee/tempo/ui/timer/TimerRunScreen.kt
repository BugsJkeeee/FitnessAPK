package com.bugsjkeeee.tempo.ui.timer

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.timer.Phase
import com.bugsjkeeee.tempo.timer.RunStatus
import com.bugsjkeeee.tempo.timer.TimerController
import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.timer.TimerSnapshot
import com.bugsjkeeee.tempo.ui.components.InfoTile
import com.bugsjkeeee.tempo.ui.components.PrimaryButton
import com.bugsjkeeee.tempo.ui.components.SecondaryButton
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.formatClock
import com.bugsjkeeee.tempo.ui.components.formatPrecise
import com.bugsjkeeee.tempo.ui.components.formatRemaining
import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import com.bugsjkeeee.tempo.ui.theme.Digits
import com.bugsjkeeee.tempo.ui.theme.Inter
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import com.bugsjkeeee.tempo.ui.theme.PhasePalette

@Composable
fun TimerRunScreen(onClose: () -> Unit, onSaveToJournal: () -> Unit) {
    val controller = (LocalContext.current.applicationContext as TempoApp).timerController
    val snapshot by controller.state.collectAsStateWithLifecycle()
    val s = snapshot
    if (s == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    if (s.status == RunStatus.FINISHED) {
        BackHandler { controller.dismiss(); onClose() }
        ResultView(s, onClose = { controller.dismiss(); onClose() }, onSave = onSaveToJournal)
    } else {
        KeepScreenOn()
        // «Назад» сворачивает экран, таймер продолжает работать.
        BackHandler(onBack = onClose)
        ActiveView(s, controller, onMinimize = onClose)
    }
}

@Composable
private fun KeepScreenOn() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

private fun PhasePalette.background(phase: Phase) = when (phase) {
    Phase.PREP -> prep
    Phase.WORK -> work
    Phase.REST -> rest
}

private fun PhasePalette.content(phase: Phase) = when (phase) {
    Phase.PREP -> onPrep
    Phase.WORK -> onWork
    Phase.REST -> onRest
}

private fun phaseName(phase: Phase) = when (phase) {
    Phase.PREP -> "Приготовьтесь"
    Phase.WORK -> "Работа"
    Phase.REST -> "Отдых"
}

private fun phaseShort(phase: Phase) = when (phase) {
    Phase.PREP -> "Подготовка"
    Phase.WORK -> "Работа"
    Phase.REST -> "Отдых"
}

/** Действие кнопки-молнии: «+1 раунд» в AMRAP, «круг» в секундомере, «финиш» в For Time. */
private fun boltAction(s: TimerSnapshot, c: TimerController): (() -> Unit)? = when {
    s.phase != Phase.WORK -> null
    s.mode == TimerMode.AMRAP || s.mode == TimerMode.STOPWATCH -> c::lap
    s.mode == TimerMode.FOR_TIME -> c::stop
    else -> null
}

private fun boltHint(s: TimerSnapshot): String? = when {
    s.phase != Phase.WORK -> null
    s.mode == TimerMode.AMRAP -> "Молния — +1 раунд"
    s.mode == TimerMode.STOPWATCH -> "Молния — отметить круг"
    s.mode == TimerMode.FOR_TIME -> "Молния — финиш"
    else -> null
}

/** Подпись под цифрами: раунд, число раундов AMRAP или кругов. */
private fun counterText(s: TimerSnapshot): String? = when {
    s.mode == TimerMode.AMRAP -> "Раундов: ${s.laps.size}"
    s.mode == TimerMode.STOPWATCH -> "Кругов: ${s.laps.size}"
    s.totalRounds > 1 -> "Раунд ${s.round} / ${s.totalRounds}"
    else -> null
}

private fun totalText(s: TimerSnapshot): String =
    "Общее " + formatClock(s.totalElapsedMs) + (s.totalDurationMs?.let { " / " + formatClock(it) } ?: "")

/** Оформление кнопок и колец для текущей фазы. */
private data class RunColors(val content: Color, val track: Color, val control: Color, val controlBorder: Color, val shadow: Boolean, val muted: Color)

@Composable
private fun ActiveView(s: TimerSnapshot, controller: TimerController, onMinimize: () -> Unit) {
    val style = LocalTempoStyle.current
    val palette = style.phases
    val background by animateColorAsState(palette.background(s.phase), tween(400), label = "phase")
    val content by animateColorAsState(palette.content(s.phase), tween(400), label = "content")
    val work = s.phase == Phase.WORK
    val colors = RunColors(
        content = content,
        track = if (work) palette.workTrack else content.copy(alpha = 0.18f),
        control = when {
            work -> palette.workControl
            else -> Color.White.copy(alpha = if (s.phase == Phase.PREP) 0.22f else 0.45f)
        },
        controlBorder = when {
            work && style.dark -> content.copy(alpha = 0.35f)
            work -> Color(0xFFE8E8E8)
            else -> Color.Transparent
        },
        shadow = work && !style.dark,
        muted = content.copy(alpha = 0.55f),
    )
    var confirmStop by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(background).systemBarsPadding()) {
        val w = maxWidth
        val h = maxHeight
        if (w > h) {
            Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Dial(s, colors, minOf(h - 24.dp, w * 0.5f))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Title(s, colors)
                    Bolt(s, controller, colors)
                    Controls(s, controller, colors) { confirmStop = true }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 30.dp).padding(top = 36.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Title(s, colors)
                Spacer(Modifier.weight(1f))
                Dial(s, colors, minOf(300.dp, w - 60.dp, h * 0.42f))
                Spacer(Modifier.weight(1f))
                Bolt(s, controller, colors)
                Spacer(Modifier.weight(1f))
                Controls(s, controller, colors) { confirmStop = true }
            }
        }
        // Свернуть экран: таймер продолжит работать в фоне.
        Icon(
            TempoIcons.ChevronDown,
            contentDescription = "Свернуть",
            tint = colors.muted,
            modifier = Modifier.padding(12.dp).size(40.dp).clip(CircleShape).clickable(onClick = onMinimize).padding(8.dp),
        )
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Остановить таймер?") },
            text = { Text("Тренировка завершится, откроется экран итога.") },
            confirmButton = { TextButton(onClick = { confirmStop = false; controller.stop() }) { Text("Стоп") } },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Продолжить") } },
        )
    }
}

private val Thin = TextStyle(fontFamily = Inter)

@Composable
private fun Title(s: TimerSnapshot, c: RunColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            phaseName(s.phase).uppercase(),
            style = Thin.copy(fontSize = 32.sp, fontWeight = FontWeight.Light, letterSpacing = 12.sp),
            color = c.content,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.width(60.dp).height(1.dp).background(c.content.copy(alpha = 0.2f)))
            Text(
                (s.mode.title + if (s.status == RunStatus.PAUSED) " · ПАУЗА" else "").uppercase(),
                style = Thin.copy(fontSize = 14.sp, letterSpacing = 8.sp),
                color = c.content,
            )
            Box(Modifier.width(60.dp).height(1.dp).background(c.content.copy(alpha = 0.2f)))
        }
    }
}

/** Круг с цифрами: дорожка 3 dp и дуга прогресса 6 dp, как в макете. */
@Composable
private fun Dial(s: TimerSnapshot, c: RunColors, size: Dp) {
    val remaining = s.segmentRemainingMs
    val text = if (remaining != null) formatRemaining(remaining) else formatClock(s.segmentElapsedMs)
    val progress = when {
        remaining != null && s.segmentDurationMs != null && s.segmentDurationMs > 0 -> remaining.toFloat() / s.segmentDurationMs
        else -> (s.segmentElapsedMs % 60_000) / 60_000f
    }
    val scale = size / 300.dp
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val track = 3.dp.toPx()
            val bar = 6.dp.toPx()
            val inset = bar / 2
            val arc = Size(this.size.width - bar, this.size.height - bar)
            drawArc(c.track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(track))
            drawArc(c.content, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arc, style = Stroke(bar, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text,
                style = Digits.copy(fontSize = (if (text.length > 5) 54 else 72).sp * scale, fontWeight = FontWeight.Normal, letterSpacing = (-2).sp),
                color = c.content,
            )
            Spacer(Modifier.height(12.dp * scale))
            Text(if (remaining != null) "ОСТАЛОСЬ" else "ПРОШЛО", style = Thin.copy(fontSize = 13.sp, letterSpacing = 6.sp), color = c.content)
            Spacer(Modifier.height(12.dp * scale))
            Box(Modifier.width(40.dp).height(1.dp).background(c.content.copy(alpha = 0.2f)))
            Spacer(Modifier.height(12.dp * scale))
            Text(
                (counterText(s) ?: "Секунд").uppercase(),
                style = Thin.copy(fontSize = 12.sp, letterSpacing = 4.sp),
                color = c.content,
            )
        }
    }
}

/** Круглая кнопка с молнией и подписи фазы под ней. */
@Composable
private fun Bolt(s: TimerSnapshot, controller: TimerController, c: RunColors) {
    val action = boltAction(s, controller)
    val paused = s.status == RunStatus.PAUSED
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .then(if (c.shadow) Modifier.shadow(8.dp, CircleShape, ambientColor = Color(0x22000000), spotColor = Color(0x22000000)) else Modifier)
                .background(c.control, CircleShape)
                .border(1.dp, c.controlBorder, CircleShape)
                .clip(CircleShape)
                .clickable(enabled = action != null && !paused) { action?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(TempoIcons.Bolt, contentDescription = boltHint(s), tint = c.content, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("ФАЗА: ${phaseShort(s.phase).uppercase()}", style = Thin.copy(fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium), color = c.content)
        Spacer(Modifier.height(6.dp))
        Text(totalText(s), style = Digits.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal), color = c.muted, textAlign = TextAlign.Center)
        val last = s.laps.lastOrNull()
        val extra = when {
            last != null && (s.mode == TimerMode.AMRAP || s.mode == TimerMode.STOPWATCH) -> {
                val lap = last - (s.laps.getOrNull(s.laps.size - 2) ?: 0L)
                (if (s.mode == TimerMode.AMRAP) "Последний раунд " else "Последний круг ") + formatPrecise(lap)
            }
            else -> boltHint(s)
        }
        if (extra != null) Text(extra, style = Digits.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal), color = c.muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Controls(s: TimerSnapshot, controller: TimerController, c: RunColors, onStop: () -> Unit) {
    val paused = s.status == RunStatus.PAUSED
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        ControlCard(if (paused) TempoIcons.Play else TempoIcons.Pause, if (paused) "Дальше" else "Пауза", c, Modifier.weight(1f), onClick = controller::togglePause)
        ControlCard(TempoIcons.Stop, "Стоп", c, Modifier.weight(1f), onClick = onStop)
        ControlCard(TempoIcons.Skip, "Пропуск", c, Modifier.weight(1f), enabled = s.segmentDurationMs != null, onClick = controller::skip)
    }
}

/** Карточка управления: иконка и подпись разрядкой, скругление 20 dp. */
@Composable
private fun ControlCard(icon: ImageVector, label: String, c: RunColors, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .widthIn(max = 100.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (c.shadow) Modifier.shadow(6.dp, shape, ambientColor = Color(0x18000000), spotColor = Color(0x18000000)) else Modifier)
            .background(c.control, shape)
            .border(BorderStroke(1.dp, c.controlBorder), shape)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = c.content, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label.uppercase(), style = Thin.copy(fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium), color = c.content, maxLines = 1)
    }
}

@Composable
private fun ResultView(s: TimerSnapshot, onClose: () -> Unit, onSave: () -> Unit) {
    val style = LocalTempoStyle.current
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        TileLabel("Итог · ${s.mode.title}", TempoIcons.Flag)
        Text("Готово", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        s.workout?.let { Text(it.name, style = MaterialTheme.typography.titleMedium, color = style.muted) }
        if (rememberNewRecord(s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(TempoIcons.Trophy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Новый рекорд!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoTile("Общее время", formatPrecise(s.totalElapsedMs), Modifier.weight(1f), TempoIcons.Timer)
            val result: Pair<String, String>? = when (s.mode) {
                TimerMode.FOR_TIME -> "Время" to formatPrecise(s.totalElapsedMs)
                TimerMode.AMRAP -> "Раунды" to s.laps.size.toString()
                TimerMode.STOPWATCH -> "Круги" to s.laps.size.toString()
                TimerMode.EMOM, TimerMode.INTERVALS -> "Раунды" to "${s.round} / ${s.totalRounds}"
                else -> null
            }
            if (result != null) InfoTile(result.first, result.second, Modifier.weight(1f), TempoIcons.Refresh)
        }
        if (s.mode.hasSplits && s.laps.isNotEmpty()) {
            Tile(Modifier.fillMaxWidth()) {
                TileLabel(if (s.mode == TimerMode.AMRAP) "Раунды" else "Круги", TempoIcons.Flag)
                Spacer(Modifier.height(6.dp))
                s.laps.forEachIndexed { i, total ->
                    val lap = total - (s.laps.getOrNull(i - 1) ?: 0L)
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text("${s.mode.splitTitle} ${i + 1}", modifier = Modifier.weight(1f), color = style.muted, style = MaterialTheme.typography.bodyMedium)
                        Text(formatPrecise(lap), style = Digits.copy(fontSize = 14.sp), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Text(formatPrecise(total), style = Digits.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal), color = style.muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        PrimaryButton("Записать в журнал", onSave, icon = TempoIcons.Journal)
        SecondaryButton("Закрыть", onClose, Modifier.fillMaxWidth())
    }
}

/** Режимы, где отмечаются круги или раунды со временем каждого. */
private val TimerMode.hasSplits get() = this == TimerMode.STOPWATCH || this == TimerMode.AMRAP
private val TimerMode.splitTitle get() = if (this == TimerMode.AMRAP) "Раунд" else "Круг"

/** Лучше ли результат комплекса, чем прежние попытки в журнале (For Time — время, AMRAP — раунды). */
@Composable
private fun rememberNewRecord(s: TimerSnapshot): Boolean {
    val app = LocalContext.current.applicationContext as TempoApp
    val record by produceState(false, s.workout?.id) {
        val ref = s.workout ?: return@produceState
        val previous = app.journalRepository.all().filter { it.workoutId == ref.id }
        value = when (s.mode) {
            TimerMode.FOR_TIME -> previous.mapNotNull { it.resultTimeMs }.minOrNull()?.let { s.totalElapsedMs < it } ?: false
            TimerMode.AMRAP -> previous.mapNotNull { it.resultRounds }.maxOrNull()?.let { s.laps.size > it } ?: false
            else -> false
        }
    }
    return record
}
