package com.bugsjkeeee.tempo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.settings.AppSettings
import com.bugsjkeeee.tempo.settings.SoundMode
import com.bugsjkeeee.tempo.sound.SoundCatalog
import com.bugsjkeeee.tempo.sound.SoundEvent
import com.bugsjkeeee.tempo.ui.appViewModel
import com.bugsjkeeee.tempo.ui.components.SegmentedSelector
import com.bugsjkeeee.tempo.ui.components.Tile
import com.bugsjkeeee.tempo.ui.components.TileLabel
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val app: TempoApp) : ViewModel() {
    private val repo = app.settingsRepository
    val settings: StateFlow<AppSettings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val voiceAvailable: Boolean get() = app.audioPlayer.voiceAvailable

    fun setSoundMode(mode: SoundMode) {
        viewModelScope.launch { repo.setSoundMode(mode) }
        if (mode == SoundMode.VOICE) app.audioPlayer.speak("Работа")
    }

    fun setSound(event: SoundEvent, key: String) {
        viewModelScope.launch { repo.setSound(event, key) }
    }

    fun preview(key: String) = app.audioPlayer.play(key)

    fun setPrep(sec: Int) {
        viewModelScope.launch { repo.setPrepSec(sec) }
    }
}

private val prepOptions = listOf(0, 5, 10, 15, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    var choosing by rememberSaveable { mutableStateOf<SoundEvent?>(null) }
    val style = LocalTempoStyle.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val s = settings ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Звуковое сопровождение", Icons.AutoMirrored.Filled.VolumeUp)
                    Spacer(Modifier.height(10.dp))
                    SegmentedSelector(SoundMode.entries, s.soundMode, { it.title }, vm::setSoundMode)
                    if (s.soundMode == SoundMode.VOICE) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (vm.voiceAvailable) {
                                "Подсказки «три, два, один», «работа», «отдых», «последний раунд», «готово» — встроенным голосом телефона."
                            } else {
                                "Русский голос в телефоне не найден — будут звучать сигналы. Установите русский язык в настройках синтеза речи Android."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = style.muted,
                        )
                    }
                }
            }
            if (s.soundMode == SoundMode.SIGNALS) {
                items(SoundEvent.entries) { event ->
                    val sound = SoundCatalog.resolve(event, s.sounds[event])
                    Tile(Modifier.fillMaxWidth(), onClick = { choosing = event }) {
                        TileLabel(event.title)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sound.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = style.muted)
                        }
                    }
                }
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Отсчёт «Приготовьтесь» перед стартом")
                    Spacer(Modifier.height(10.dp))
                    SegmentedSelector(prepOptions, s.prepSec, { if (it == 0) "нет" else "$it с" }, vm::setPrep)
                }
            }
        }
    }

    val event = choosing
    val current = settings
    if (event != null && current != null) {
        AlertDialog(
            onDismissRequest = { choosing = null },
            title = { Text(event.title) },
            text = {
                Column {
                    SoundCatalog.forEvent(event).forEach { sound ->
                        val selected = current.sounds[event] == sound.key
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.setSound(event, sound.key)
                                    vm.preview(sound.key)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = {
                                vm.setSound(event, sound.key)
                                vm.preview(sound.key)
                            })
                            Text(sound.title, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.preview(sound.key) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Прослушать")
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = null }) { Text("Готово") } },
        )
    }
}
