package com.bugsjkeeee.tempo.ui.timer

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.timer.Phase
import com.bugsjkeeee.tempo.timer.RunStatus
import com.bugsjkeeee.tempo.timer.TimerController
import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.timer.TimerService
import com.bugsjkeeee.tempo.timer.TimerSnapshot
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.formatClock
import com.bugsjkeeee.tempo.ui.components.formatPrecise
import com.bugsjkeeee.tempo.ui.components.formatRemaining
import com.bugsjkeeee.tempo.ui.theme.Digits
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import com.bugsjkeeee.tempo.ui.theme.PhaseColors

@Composable
fun TimerRunScreen(onClose: () -> Unit) {
    val controller = (LocalContext.current.applicationContext as TempoApp).timerController
    val snapshot by controller.state.collectAsStateWithLifecycle()
    val s = snapshot
    if (s == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    if (s.status == RunStatus.FINISHED) {
        BackHandler { controller.dismiss(); onClose() }
        ResultView(s, onClose = { controller.dismiss(); onClose() })
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

private fun phaseColor(phase: Phase) = when (phase) {
    Phase.PREP -> PhaseColors.Prep
    Phase.WORK -> PhaseColors.Work
    Phase.REST -> PhaseColors.Rest
}

private fun onPhaseColor(phase: Phase) = when (phase) {
    Phase.PREP -> PhaseColors.OnPrep
    Phase.WORK -> PhaseColors.OnWork
    Phase.REST -> PhaseColors.OnRest
}

@Composable
private fun ActiveView(s: TimerSnapshot, controller: TimerController, onMinimize: () -> Unit) {
    val background by animateColorAsState(phaseColor(s.phase), tween(400), label = "phase")
    val content = onPhaseColor(s.phase)
    var confirmStop by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(background).systemBarsPadding()) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Dial(s, content, minOf(maxHeight - 32.dp, maxWidth * 0.55f))
                }
                Column(
                    Modifier.weight(1f).padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Header(s, content, onMinimize)
                    Info(s, content)
                    Controls(s, controller, content, onStop = { confirmStop = true })
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(s, content, onMinimize)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Dial(s, content, minOf(maxWidth - 16.dp, maxHeight * 0.5f))
                }
                Info(s, content)
                Spacer(Modifier.height(16.dp))
                Controls(s, controller, content, onStop = { confirmStop = true })
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Остановить таймер?") },
            text = { Text("Тренировка завершится, откроется экран итога.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    controller.stop()
                }) { Text("Стоп") }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Продолжить") } },
        )
    }
}

@Composable
private fun Header(s: TimerSnapshot, content: Color, onMinimize: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinimize) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Свернуть", tint = content)
        }
        Column(Modifier.weight(1f)) {
            Text(
                TimerService.phaseTitle(s.phase).uppercase(),
                color = content,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            )
            Text(
                s.mode.title + if (s.status == RunStatus.PAUSED) " · пауза" else "",
                color = content.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Крупный циферблат: цифры текущего интервала и круговой индикатор вокруг них. */
@Composable
private fun Dial(s: TimerSnapshot, content: Color, size: Dp) {
    val remaining = s.segmentRemainingMs
    val text = if (remaining != null) formatRemaining(remaining) else formatClock(s.segmentElapsedMs)
    val progress = when {
        remaining != null && s.segmentDurationMs != null && s.segmentDurationMs > 0 ->
            remaining.toFloat() / s.segmentDurationMs
        else -> (s.segmentElapsedMs % 60_000) / 60_000f
    }
    val density = LocalDensity.current
    // Ширина «00:00» примерно 2,8 высоты шрифта; цифры занимают ~3/4 диаметра круга.
    val fontScale = if (text.length > 5) 0.2f else 0.27f
    val fontSize = with(density) { (size * fontScale).toSp() }

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.045f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(content.copy(alpha = 0.22f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                content, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(text, color = content, style = Digits.copy(fontSize = fontSize, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun Info(s: TimerSnapshot, content: Color) {
    val big = MaterialTheme.typography.headlineMedium.merge(Digits)
    val small = MaterialTheme.typography.titleMedium.merge(Digits)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        val roundText = when {
            s.mode == TimerMode.AMRAP -> "Раунды: ${s.amrapRounds}"
            s.mode == TimerMode.STOPWATCH -> "Круги: ${s.laps.size}"
            s.totalBlocks > 1 && s.totalRounds > 0 -> "Блок ${s.block}/${s.totalBlocks} · Раунд ${s.round} / ${s.totalRounds}"
            s.totalBlocks > 1 -> "Отдых перед блоком ${s.block + 1}"
            s.totalRounds > 1 -> "Раунд ${s.round} / ${s.totalRounds}"
            else -> null
        }
        if (roundText != null) Text(roundText, color = content, style = big)
        val total = "Общее " + formatClock(s.totalElapsedMs) + (s.totalDurationMs?.let { " / " + formatClock(it) } ?: "")
        Text(total, color = content.copy(alpha = 0.8f), style = small)
        if (s.mode == TimerMode.STOPWATCH && s.laps.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val laps = s.laps.mapIndexed { i, t -> Triple(i + 1, t - (s.laps.getOrNull(i - 1) ?: 0L), t) }
            laps.takeLast(4).reversed().forEach { (n, lap, total) ->
                Text(
                    "Круг $n   ${formatPrecise(lap)}   ${formatPrecise(total)}",
                    color = content.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge.merge(Digits),
                )
            }
        }
    }
}

@Composable
private fun Controls(s: TimerSnapshot, controller: TimerController, content: Color, onStop: () -> Unit) {
    val background = phaseColor(s.phase)
    val primaryColors = ButtonDefaults.buttonColors(containerColor = content, contentColor = background)
    val paused = s.status == RunStatus.PAUSED
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (s.mode) {
            TimerMode.AMRAP -> if (s.phase == Phase.WORK) {
                Button(onClick = controller::addRound, colors = primaryColors, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    Text("+1 раунд", style = MaterialTheme.typography.headlineMedium)
                }
            }
            TimerMode.STOPWATCH -> if (s.phase == Phase.WORK) {
                Button(onClick = controller::lap, colors = primaryColors, modifier = Modifier.fillMaxWidth().height(64.dp), enabled = !paused) {
                    Icon(Icons.Filled.Flag, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Круг", style = MaterialTheme.typography.titleLarge)
                }
            }
            TimerMode.FOR_TIME -> if (s.phase == Phase.WORK) {
                Button(onClick = controller::stop, colors = primaryColors, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text("Финиш", style = MaterialTheme.typography.titleLarge)
                }
            }
            else -> Unit
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, content.copy(alpha = 0.6f)),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Стоп")
            }
            Button(
                onClick = controller::togglePause,
                colors = primaryColors,
                modifier = Modifier.weight(1.6f).height(56.dp),
            ) {
                Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (paused) "Продолжить" else "Пауза", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = controller::skip,
                enabled = s.segmentDurationMs != null,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = content, disabledContentColor = content.copy(alpha = 0.3f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, content.copy(alpha = 0.6f)),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Пропустить интервал")
            }
        }
    }
}

@Composable
private fun ResultView(s: TimerSnapshot, onClose: () -> Unit) {
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
        Spacer(Modifier.height(16.dp))
        TileLabel("Итог · ${s.mode.title}")
        Text("Готово", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        val big = Digits.copy(fontSize = 40.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(Modifier.weight(1f)) {
                TileLabel("Общее время")
                Text(formatPrecise(s.totalElapsedMs), style = big.copy(fontSize = 30.sp), color = MaterialTheme.colorScheme.onSurface)
            }
            val result: Pair<String, String>? = when (s.mode) {
                TimerMode.FOR_TIME -> "Время" to formatPrecise(s.totalElapsedMs)
                TimerMode.AMRAP -> "Раунды" to s.amrapRounds.toString()
                TimerMode.STOPWATCH -> "Круги" to s.laps.size.toString()
                TimerMode.EMOM, TimerMode.INTERVALS -> "Раунды" to "${s.round} / ${s.totalRounds}"
                else -> null
            }
            if (result != null) {
                Tile(Modifier.weight(1f)) {
                    TileLabel(result.first)
                    Text(result.second, style = big.copy(fontSize = 30.sp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (s.mode == TimerMode.STOPWATCH && s.laps.isNotEmpty()) {
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Круги")
                Spacer(Modifier.height(8.dp))
                s.laps.forEachIndexed { i, total ->
                    val lap = total - (s.laps.getOrNull(i - 1) ?: 0L)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Круг ${i + 1}", modifier = Modifier.weight(1f), color = style.muted)
                        Text(formatPrecise(lap), style = Digits, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Text(formatPrecise(total), style = Digits, color = style.muted)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(56.dp), shape = style.tileShape) {
            Text("Записать в журнал")
        }
        Text("Журнал появится на этапе 3.", style = MaterialTheme.typography.bodySmall, color = style.muted)
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp), shape = style.tileShape) {
            Text("Закрыть", style = MaterialTheme.typography.titleMedium)
        }
    }
}
