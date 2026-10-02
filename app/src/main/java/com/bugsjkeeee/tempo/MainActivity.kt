package com.bugsjkeeee.tempo

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugsjkeeee.tempo.settings.AppSettings
import com.bugsjkeeee.tempo.settings.ThemeMode
import com.bugsjkeeee.tempo.ui.TempoNavHost
import com.bugsjkeeee.tempo.ui.theme.TempoTheme

class MainActivity : ComponentActivity() {
    /** Действие из виджета; сбрасывается после обработки. */
    private val widgetAction = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) widgetAction.value = intent?.getStringExtra(TempoWidget.EXTRA_ACTION)
        enableEdgeToEdge()
        val settings = (application as TempoApp).settingsRepository.settings
        setContent {
            val s by settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (s.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            // Цвет значков строки состояния следует теме приложения, а не только системной.
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            TempoTheme(darkTheme = dark) {
                TempoNavHost(widgetAction = widgetAction.value, onWidgetActionHandled = { widgetAction.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(TempoWidget.EXTRA_ACTION)?.let { widgetAction.value = it }
    }
}
