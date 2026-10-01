package com.bugsjkeeee.tempo.ui.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.timer.Block
import com.bugsjkeeee.tempo.timer.Interval
import com.bugsjkeeee.tempo.timer.Phase
import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.timer.TimerSettings
import com.bugsjkeeee.tempo.timer.TimerSnapshot
import com.bugsjkeeee.tempo.timer.buildPlan
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.CountStepper
import com.bugsjkeeee.tempo.ui.components.DurationStepper
import com.bugsjkeeee.tempo.ui.components.SegmentedSelector
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.formatClock
import com.bugsjkeeee.tempo.ui.theme.Digits
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerSetupState(
    val loaded: Boolean = false,
    val mode: TimerMode = TimerMode.INTERVALS,
    val settings: TimerSettings = TimerSettings(),
    val prepSec: Int = 10,
)

class TimerSetupViewModel(private val app: TempoApp) : ViewModel() {
    private val repo = app.settingsRepository

    val state: StateFlow<TimerSetupState> = combine(repo.lastMode, repo.timerSettings, repo.settings) { mode, timer, app ->
        TimerSetupState(true, mode, timer, app.prepSec)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerSetupState())

    val running: StateFlow<TimerSnapshot?> = app.timerController.state

    fun selectMode(mode: TimerMode) {
        viewModelScope.launch { repo.setLastMode(mode) }
    }

    fun update(transform: (TimerSettings) -> TimerSettings) {
        viewModelScope.launch { repo.saveTimerSettings(transform(state.value.settings)) }
    }

    fun start() {
        val s = state.value
        app.timerController.start(s.mode, s.settings)
    }
}

private val modeHints = mapOf(
    TimerMode.STOPWATCH to "Счёт от нуля без ограничения, отметка кругов.",
    TimerMode.COUNTDOWN to "Простой обратный отсчёт со звуком в конце.",
    TimerMode.FOR_TIME to "Комплекс на время: секундомер до кнопки «Финиш» или до лимита.",
    TimerMode.AMRAP to "Как можно больше раундов за заданное время. Отмечайте круги кнопкой «+1 раунд».",
    TimerMode.EMOM to "Сигнал в начале каждого интервала, заданное число раундов.",
    TimerMode.INTERVALS to "Чередование работы и отдыха: Табата, 20/40, 2 минуты × 10 и т.п.",
    TimerMode.CUSTOM to "Свой сценарий из блоков интервалов, например разминка и несколько блоков работы с отдыхом между ними.",
)

@Composable
fun TimerSetupScreen(onOpenRunning: () -> Unit, onStarted: () -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel { TimerSetupViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    if (!state.loaded) return
    val s = state.settings
    val style = LocalTempoStyle.current
    val context = LocalContext.current
    // Android 13+: без разрешения таймер работает, но не виден в шторке.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (running != null) {
            item {
                Tile(Modifier.fillMaxWidth(), onClick = onOpenRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Таймер запущен", style = MaterialTheme.typography.titleMedium)
                            Text(running!!.mode.title, color = style.muted)
                        }
                        Text("Открыть", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Режим")
                Spacer(Modifier.height(10.dp))
                SegmentedSelector(TimerMode.entries, state.mode, { it.title }, { vm.selectMode(it) })
                Spacer(Modifier.height(10.dp))
                Text(modeHints.getValue(state.mode), style = MaterialTheme.typography.bodyMedium, color = style.muted)
            }
        }

        item {
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Параметры")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeParameters(state.mode, s, vm::update)
                }
            }
        }

        if (state.mode == TimerMode.CUSTOM) {
            customScenarioItems(s, vm::update)
        }

        item {
            val plan = buildPlan(state.mode, s, 0)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(Modifier.weight(1f)) {
                    TileLabel("Общее время")
                    Text(
                        plan.totalMs?.let(::formatClock) ?: "∞",
                        style = Digits.copy(fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Tile(Modifier.weight(1f)) {
                    TileLabel("Подготовка")
                    Text(
                        if (state.prepSec > 0) "${state.prepSec} с" else "нет",
                        style = Digits.copy(fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.start()
                    onStarted()
                },
                enabled = running == null,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = style.tileShape,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Старт", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ModeParameters(mode: TimerMode, s: TimerSettings, update: ((TimerSettings) -> TimerSettings) -> Unit) {
    when (mode) {
        TimerMode.STOPWATCH -> Text(
            "Настраивать нечего — нажмите «Старт».",
            color = LocalTempoStyle.current.muted,
        )
        TimerMode.COUNTDOWN -> DurationStepper("Длительность", s.countdownSec, { v -> update { it.copy(countdownSec = v) } })
        TimerMode.FOR_TIME -> DurationStepper(
            "Лимит времени", s.forTimeCapSec, { v -> update { it.copy(forTimeCapSec = v) } },
            min = 0, zeroText = "нет",
        )
        TimerMode.AMRAP -> DurationStepper("Длительность", s.amrapSec, { v -> update { it.copy(amrapSec = v) } })
        TimerMode.EMOM -> {
            DurationStepper("Интервал", s.emomIntervalSec, { v -> update { it.copy(emomIntervalSec = v) } })
            CountStepper("Раунды", s.emomRounds, { v -> update { it.copy(emomRounds = v) } })
        }
        TimerMode.INTERVALS -> {
            DurationStepper("Работа", s.workSec, { v -> update { it.copy(workSec = v) } })
            DurationStepper("Отдых", s.restSec, { v -> update { it.copy(restSec = v) } }, min = 0, zeroText = "нет")
            CountStepper("Раунды", s.intervalRounds, { v -> update { it.copy(intervalRounds = v) } })
        }
        TimerMode.CUSTOM -> DurationStepper(
            "Отдых между блоками", s.customRestBetweenBlocksSec,
            { v -> update { it.copy(customRestBetweenBlocksSec = v) } },
            min = 0, zeroText = "нет",
        )
    }
}

/** Редактор своего сценария: блоки с интервалами и числом повторов. */
private fun androidx.compose.foundation.lazy.LazyListScope.customScenarioItems(
    s: TimerSettings,
    update: ((TimerSettings) -> TimerSettings) -> Unit,
) {
    fun setBlocks(transform: (MutableList<Block>) -> Unit) = update {
        it.copy(customBlocks = it.customBlocks.toMutableList().also(transform))
    }

    s.customBlocks.forEachIndexed { b, block ->
        item(key = "block_$b") {
            Tile(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TileLabel("Блок ${b + 1}", modifier = Modifier.weight(1f))
                    if (s.customBlocks.size > 1) {
                        IconButton(onClick = { setBlocks { it.removeAt(b) } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Удалить блок")
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    block.intervals.forEachIndexed { i, interval ->
                        IntervalRow(
                            interval = interval,
                            canDelete = block.intervals.size > 1,
                            onChange = { new ->
                                setBlocks { list ->
                                    list[b] = block.copy(intervals = block.intervals.toMutableList().also { it[i] = new })
                                }
                            },
                            onDelete = {
                                setBlocks { list ->
                                    list[b] = block.copy(intervals = block.intervals.filterIndexed { j, _ -> j != i })
                                }
                            },
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            setBlocks { list ->
                                val next = if (block.intervals.last().phase == Phase.WORK) Interval(Phase.REST, 20) else Interval(Phase.WORK, 40)
                                list[b] = block.copy(intervals = block.intervals + next)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Интервал")
                    }
                    CountStepper("Повторов блока", block.repeats, { v -> setBlocks { it[b] = block.copy(repeats = v) } })
                }
            }
        }
    }
    item(key = "add_block") {
        OutlinedButton(
            onClick = { setBlocks { it.add(Block(listOf(Interval(Phase.WORK, 40), Interval(Phase.REST, 20)), 8)) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Блок")
        }
    }
}

@Composable
private fun IntervalRow(interval: Interval, canDelete: Boolean, onChange: (Interval) -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedSelector(
                options = listOf(Phase.WORK, Phase.REST),
                selected = interval.phase,
                label = { if (it == Phase.WORK) "Работа" else "Отдых" },
                onSelect = { onChange(interval.copy(phase = it)) },
                modifier = Modifier.weight(1f),
            )
            if (canDelete) {
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = "Удалить интервал") }
            }
        }
        DurationStepper("Длительность", interval.seconds, { onChange(interval.copy(seconds = it)) })
    }
}
