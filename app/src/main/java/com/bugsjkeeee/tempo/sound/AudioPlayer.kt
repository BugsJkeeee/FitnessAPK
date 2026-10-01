package com.bugsjkeeee.tempo.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import com.bugsjkeeee.tempo.settings.AppSettings
import com.bugsjkeeee.tempo.settings.SoundMode
import com.bugsjkeeee.tempo.timer.TimerEvent
import java.util.Locale

/**
 * Проигрывает сигналы таймера или голосовые подсказки.
 * На время звука запрашивает «временный фокус с приглушением», поэтому музыка
 * из других приложений становится тише и затем возвращается к прежней громкости.
 */
class AudioPlayer(private val context: Context) {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
    private val soundIds = mutableMapOf<String, Int>()
    private val durationsMs = mutableMapOf<String, Long>()

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { }
        .build()
    private val handler = Handler(Looper.getMainLooper())
    private var hasFocus = false
    private var focusUntil = 0L

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        SoundCatalog.all.forEach { sound ->
            soundIds[sound.key] = soundPool.load(context, sound.res, 1)
            durationsMs[sound.key] = wavDurationMs(sound.res)
        }
    }

    /** Есть ли русский голос; если нет — используются сигналы. */
    val voiceAvailable: Boolean get() = ttsReady

    fun prepareVoice() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            val engine = tts ?: return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                val result = engine.setLanguage(Locale("ru", "RU"))
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                engine.setAudioAttributes(attributes)
            }
        }
    }

    fun handle(event: TimerEvent, settings: AppSettings) {
        if (settings.soundMode == SoundMode.VOICE && ttsReady) {
            phrase(event)?.let(::speak)
            return
        }
        val soundEvent = when (event) {
            is TimerEvent.Countdown -> SoundEvent.COUNTDOWN
            is TimerEvent.WorkStart -> SoundEvent.WORK
            TimerEvent.RestStart -> SoundEvent.REST
            TimerEvent.Finish -> SoundEvent.FINISH
            TimerEvent.PrepStart -> return
        }
        play(SoundCatalog.resolve(soundEvent, settings.sounds[soundEvent]).key)
    }

    fun play(key: String) {
        val id = soundIds[key] ?: return
        holdFocus(durationsMs[key] ?: 1000L)
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun speak(text: String) {
        val engine = tts ?: return
        holdFocus(1500L)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    private fun phrase(event: TimerEvent): String? = when (event) {
        is TimerEvent.Countdown -> when (event.secondsLeft) {
            3 -> "три"
            2 -> "два"
            else -> "один"
        }
        is TimerEvent.WorkStart -> if (event.lastRound) "Последний раунд" else "Работа"
        TimerEvent.RestStart -> "Отдых"
        TimerEvent.PrepStart -> "Приготовьтесь"
        TimerEvent.Finish -> "Готово"
    }

    private fun holdFocus(durationMs: Long) {
        focusUntil = maxOf(focusUntil, SystemClock.uptimeMillis() + durationMs + 300)
        if (!hasFocus) {
            hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        handler.removeCallbacks(releaseFocus)
        handler.postAtTime(releaseFocus, focusUntil)
    }

    private val releaseFocus = Runnable {
        if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }

    /** Длительность WAV (моно, 16 бит, 22 050 Гц) по размеру ресурса. */
    private fun wavDurationMs(res: Int): Long = runCatching {
        context.resources.openRawResourceFd(res).use { fd -> (fd.length - 44) * 1000 / (22050 * 2) }
    }.getOrDefault(1000L)

    fun release() {
        soundPool.release()
        tts?.shutdown()
        releaseFocus.run()
    }
}
