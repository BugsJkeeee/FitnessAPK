package com.bugsjkeeee.tempo.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.bugsjkeeee.tempo.MainActivity
import com.bugsjkeeee.tempo.R
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.ui.components.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Служба переднего плана: не даёт системе остановить таймер, когда приложение свёрнуто
 * или экран заблокирован, и показывает в шторке фазу, время и кнопки управления.
 */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private val controller get() = (application as TempoApp).timerController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(controller.state.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        // Частичная блокировка сна держит отсчёт точным при выключенном экране.
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tempo:timer")
            .apply { acquire(4 * 60 * 60 * 1000L) }

        scope.launch {
            controller.state.filterNotNull()
                .map { notificationKey(it) to it }
                .distinctUntilChanged { a, b -> a.first == b.first }
                .collect { (_, snapshot) ->
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(snapshot))
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> controller.togglePause()
            ACTION_STOP -> controller.stop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    /** Уведомление обновляется только при смене отображаемой секунды или состояния. */
    private fun notificationKey(s: TimerSnapshot) =
        "${s.status}|${s.phase}|${s.round}|${(s.segmentRemainingMs ?: s.segmentElapsedMs) / 1000}"

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Таймер", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(s: TimerSnapshot?): android.app.Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (s == null) return builder.setContentTitle("Tempo").build()

        val time = formatClock(s.segmentRemainingMs ?: s.segmentElapsedMs)
        val rounds = if (s.totalRounds > 1) " · раунд ${s.round}/${s.totalRounds}" else ""
        val paused = s.status == RunStatus.PAUSED
        return builder
            .setContentTitle("${phaseTitle(s.phase)} · $time")
            .setContentText(s.mode.title + rounds + if (paused) " · пауза" else "")
            .addAction(0, if (paused) "Продолжить" else "Пауза", actionIntent(ACTION_TOGGLE, 1))
            .addAction(0, "Стоп", actionIntent(ACTION_STOP, 2))
            .build()
    }

    private fun actionIntent(action: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, TimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_TOGGLE = "com.bugsjkeeee.tempo.TOGGLE"
        private const val ACTION_STOP = "com.bugsjkeeee.tempo.STOP"

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }

        fun phaseTitle(phase: Phase) = when (phase) {
            Phase.PREP -> "Приготовьтесь"
            Phase.WORK -> "Работа"
            Phase.REST -> "Отдых"
        }
    }
}
