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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.timer.TimerMode
import com.bugsjkeeee.tempo.timer.TimerSettings
import com.bugsjkeeee.tempo.timer.TimerSnapshot
import com.bugsjkeeee.tempo.timer.buildPlan
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.CountStepper
import com.bugsjkeeee.tempo.ui.components.DurationStepper
import com.bugsjkeeee.tempo.ui.components.InfoTile
import com.bugsjkeeee.tempo.ui.components.Note
import com.bugsjkeeee.tempo.ui.components.NumberWheelDialog
import com.bugsjkeeee.tempo.ui.components.PrimaryButton
import com.bugsjkeeee.tempo.ui.components.SegmentedSelector
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.components.formatClock
import com.bugsjkeeee.tempo.ui.icons.TempoIcons
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

    fun setPrep(sec: Int) {
        viewModelScope.launch { repo.setPrepSec(sec) }
    }

    fun start() {
        val s = state.value
        app.timerController.start(s.mode, s.settings)
    }
}

private val modeHints = mapOf(
    TimerMode.STOPWATCH to "Счёт от нуля без ограничения, отметка кругов кнопкой-молнией.",
    TimerMode.COUNTDOWN to "Простой обратный отсчёт со звуком в конце.",
    TimerMode.FOR_TIME to "Комплекс на время: секундомер до финиша (кнопка-молния) или до лимита.",
    TimerMode.AMRAP to "Как можно больше раундов за заданное время. Каждый раунд отмечайте кнопкой-молнией — увидите время раунда.",
    TimerMode.EMOM to "Сигнал в начале каждого интервала, заданное число раундов.",
    TimerMode.INTERVALS to "Чередование работы и отдыха: Табата, 20/40, 2 минуты × 10 и т.п.",
)

@Composable
fun TimerSetupScreen(onOpenRunning: () -> Unit, onStarted: () -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel { TimerSetupViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    var editPrep by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    // Android 13+: без разрешения таймер работает, но не виден в шторке.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    if (!state.loaded) return
    val s = state.settings
    val style = LocalTempoStyle.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        running?.let { r ->
            item {
                Tile(Modifier.fillMaxWidth(), onClick = onOpenRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(TempoIcons.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Таймер запущен", style = MaterialTheme.typography.titleMedium)
                            Text(r.mode.title, color = style.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(TempoIcons.ChevronRight, contentDescription = "Открыть", tint = style.muted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        item {
            Tile(Modifier.fillMaxWidth()) {
                TileLabel("Режим", TempoIcons.Play)
                Spacer(Modifier.height(8.dp))
                SegmentedSelector(TimerMode.entries, state.mode, { it.title }, { vm.selectMode(it) }, columns = 3)
                Spacer(Modifier.height(12.dp))
                Note(modeHints.getValue(state.mode))
            }
        }

        if (state.mode != TimerMode.STOPWATCH) {
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Параметры", TempoIcons.Settings)
                    ModeParameters(state.mode, s, vm::update)
                }
            }
        }

        item {
            val plan = buildPlan(state.mode, s, 0)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoTile("Общее время", plan.totalMs?.let(::formatClock) ?: "∞", Modifier.weight(1f), TempoIcons.Timer)
                InfoTile(
                    "Подготовка",
                    if (state.prepSec > 0) "${state.prepSec} с" else "нет",
                    Modifier.weight(1f),
                    TempoIcons.Bell,
                    onClick = { editPrep = true },
                )
            }
        }

        item {
            PrimaryButton(
                "Старт",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.start()
                    onStarted()
                },
                icon = TempoIcons.Play,
                enabled = running == null,
            )
        }
    }

    if (editPrep) {
        NumberWheelDialog("Подготовка перед стартом", state.prepSec, 0, 60, label = "сек", onDismiss = { editPrep = false }) {
            editPrep = false
            vm.setPrep(it)
        }
    }
}

@Composable
private fun ModeParameters(mode: TimerMode, s: TimerSettings, update: ((TimerSettings) -> TimerSettings) -> Unit) {
    Column {
        when (mode) {
            TimerMode.STOPWATCH -> Unit
            TimerMode.COUNTDOWN -> DurationStepper("Длительность", s.countdownSec, { v -> update { it.copy(countdownSec = v) } }, sub = "Обратный отсчёт")
            TimerMode.FOR_TIME -> DurationStepper(
                "Лимит времени", s.forTimeCapSec, { v -> update { it.copy(forTimeCapSec = v) } },
                min = 0, zeroText = "нет", sub = "Необязательно",
            )
            TimerMode.AMRAP -> DurationStepper("Длительность", s.amrapSec, { v -> update { it.copy(amrapSec = v) } }, sub = "Время на раунды")
            TimerMode.EMOM -> {
                DurationStepper("Интервал", s.emomIntervalSec, { v -> update { it.copy(emomIntervalSec = v) } }, sub = "Сигнал каждые")
                CountStepper("Раунды", s.emomRounds, { v -> update { it.copy(emomRounds = v) } }, sub = "Количество кругов")
            }
            TimerMode.INTERVALS -> {
                DurationStepper("Работа", s.workSec, { v -> update { it.copy(workSec = v) } }, icon = TempoIcons.Bolt)
                DurationStepper("Отдых", s.restSec, { v -> update { it.copy(restSec = v) } }, min = 0, zeroText = "нет")
                CountStepper("Раунды", s.intervalRounds, { v -> update { it.copy(intervalRounds = v) } }, sub = "Количество кругов")
            }
        }
    }
}
