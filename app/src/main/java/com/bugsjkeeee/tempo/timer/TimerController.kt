package com.bugsjkeeee.tempo.timer

import android.content.Context
import android.os.SystemClock
import com.bugsjkeeee.tempo.settings.AppSettings
import com.bugsjkeeee.tempo.settings.SettingsRepository
import com.bugsjkeeee.tempo.sound.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RunStatus { RUNNING, PAUSED, FINISHED }

/** Снимок состояния таймера для экрана и уведомления. */
data class TimerSnapshot(
    val mode: TimerMode,
    val status: RunStatus,
    val phase: Phase,
    /** Прошло в текущем отрезке. */
    val segmentElapsedMs: Long,
    /** Длительность текущего отрезка или null для открытого. */
    val segmentDurationMs: Long?,
    val round: Int,
    val totalRounds: Int,
    val block: Int,
    val totalBlocks: Int,
    /** Общее время без подготовки. */
    val totalElapsedMs: Long,
    val totalDurationMs: Long?,
    val laps: List<Long>,
    val amrapRounds: Int,
) {
    val segmentRemainingMs: Long? get() = segmentDurationMs?.let { (it - segmentElapsedMs).coerceAtLeast(0) }
}

/**
 * Единственный на приложение запуск таймера. Живёт в процессе приложения,
 * а [TimerService] держит процесс активным, пока таймер не остановлен.
 */
class TimerController(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val audio: AudioPlayer,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<TimerSnapshot?>(null)
    val state: StateFlow<TimerSnapshot?> = _state.asStateFlow()

    private var run: TimerRun? = null
    private var settings = AppSettings()
    private var ticker: Job? = null

    private fun now() = SystemClock.elapsedRealtime()

    fun start(mode: TimerMode, timerSettings: TimerSettings) {
        scope.launch {
            settings = settingsRepository.current()
            val plan = buildPlan(mode, timerSettings, settings.prepSec)
            val newRun = TimerRun(plan, now())
            run = newRun
            audio.handle(plan.startEvent(0), settings)
            publish()
            TimerService.start(context)
            startTicker()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val r = run ?: break
                r.poll(now()).forEach { audio.handle(it, settings) }
                publish()
                if (r.isFinished) {
                    TimerService.stop(context)
                    break
                }
                delay(50)
            }
        }
    }

    fun pause() {
        val r = run ?: return
        r.pause(now())
        publish()
    }

    fun resume() {
        val r = run ?: return
        r.resume(now())
        publish()
    }

    fun togglePause() {
        val r = run ?: return
        if (r.isPaused) resume() else pause()
    }

    fun skip() {
        val r = run ?: return
        r.skip(now())?.let { audio.handle(it, settings) }
        publish()
    }

    fun lap() {
        run?.lap(now())
        publish()
    }

    fun addRound() {
        run?.addRound()
        publish()
    }

    /** «Стоп» и «Финиш»: фиксирует результат и открывает экран итога. */
    fun stop() {
        val r = run ?: return
        if (!r.isFinished) {
            r.finish(now())
            audio.handle(TimerEvent.Finish, settings)
        }
        publish()
        ticker?.cancel()
        TimerService.stop(context)
    }

    /** Закрывает экран итога. */
    fun dismiss() {
        ticker?.cancel()
        run = null
        _state.value = null
        TimerService.stop(context)
    }

    private fun publish() {
        val r = run ?: return
        val t = now()
        val plan = r.plan
        val elapsed = r.elapsed(t)
        val pos = plan.positionAt(elapsed)
        val segment = plan.segments[pos.index]
        _state.value = TimerSnapshot(
            mode = plan.mode,
            status = when {
                r.isFinished -> RunStatus.FINISHED
                r.isPaused -> RunStatus.PAUSED
                else -> RunStatus.RUNNING
            },
            phase = segment.phase,
            segmentElapsedMs = pos.segmentElapsedMs,
            segmentDurationMs = segment.durationMs,
            round = segment.round,
            totalRounds = segment.totalRounds,
            block = segment.block,
            totalBlocks = segment.totalBlocks,
            totalElapsedMs = (elapsed - plan.prepMs).coerceAtLeast(0),
            totalDurationMs = plan.totalMs?.minus(plan.prepMs),
            laps = r.laps.toList(),
            amrapRounds = r.amrapRounds,
        )
    }
}
