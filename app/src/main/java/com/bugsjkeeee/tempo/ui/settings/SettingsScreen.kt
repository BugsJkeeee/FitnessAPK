package com.bugsjkeeee.tempo.ui.settings

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import com.bugsjkeeee.tempo.ui.components.TButton
import com.bugsjkeeee.tempo.ui.components.TOutlinedButton
import com.bugsjkeeee.tempo.ui.components.TOutlinedTextField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardType
import com.bugsjkeeee.tempo.ui.components.formatWeight
import com.bugsjkeeee.tempo.ui.components.parseDecimal
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.settings.AppSettings
import com.bugsjkeeee.tempo.settings.SoundMode
import com.bugsjkeeee.tempo.settings.ThemeMode
import com.bugsjkeeee.tempo.ui.base.BackTopBar
import com.bugsjkeeee.tempo.ui.components.RoundIconButton
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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setPrep(sec: Int) {
        viewModelScope.launch { repo.setPrepSec(sec) }
    }

    fun setTargetWeight(weight: Double?) {
        viewModelScope.launch { repo.setTargetWeight(weight) }
    }

    suspend fun shareIntent(): android.content.Intent = app.backupManager.shareIntent()

    /** Восстанавливает данные из файла; возвращает текст ошибки или null при успехе. */
    suspend fun restore(uri: android.net.Uri): String? = runCatching {
        val text = app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("empty")
        app.backupManager.restore(text)
    }.exceptionOrNull()?.let { "Не удалось прочитать файл — это не резервная копия Tempo или файл повреждён." }
}

private val prepOptions = listOf(0, 5, 10, 15, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    var choosing by rememberSaveable { mutableStateOf<SoundEvent?>(null) }
    val style = LocalTempoStyle.current
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingRestore = uri }

    Scaffold(
        topBar = {
            BackTopBar("Настройки", onBack)
        },
    ) { padding ->
        val s = settings ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Тема оформления", TempoIcons.Sun)
                    Spacer(Modifier.height(10.dp))
                    SegmentedSelector(ThemeMode.entries, s.themeMode, { it.title }, vm::setThemeMode)
                }
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Звуковое сопровождение", TempoIcons.Volume)
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
                            Icon(TempoIcons.ChevronRight, contentDescription = null, tint = style.muted)
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
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Целевой вес")
                    Spacer(Modifier.height(8.dp))
                    var text by rememberSaveable { mutableStateOf(s.targetWeight?.let(::formatWeight).orEmpty()) }
                    TOutlinedTextField(
                        value = text,
                        onValueChange = { v ->
                            text = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
                            vm.setTargetWeight(parseDecimal(text)?.takeIf { it in 20.0..400.0 })
                        },
                        label = { Text("кг") },
                        placeholder = { Text("не задан") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Показывается линией на графике веса.", style = MaterialTheme.typography.bodySmall, color = style.muted)
                }
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    TileLabel("Резервная копия")
                    Spacer(Modifier.height(8.dp))
                    TOutlinedButton(
                        onClick = { scope.launch { context.startActivity(android.content.Intent.createChooser(vm.shareIntent(), "Сохранить копию")) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Сохранить копию") }
                    TOutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Восстановить из копии") }
                    Text(
                        "Копия — файл со всеми данными: журнал, вес, свои тренировки, избранное и настройки. " +
                            "Отправьте его на Google Диск или себе в Telegram. Раз в неделю приложение само сохраняет копию в «Загрузки/Tempo» (Android 10 и новее).",
                        style = MaterialTheme.typography.bodySmall,
                        color = style.muted,
                    )
                }
            }
            item {
                Text(
                    "Версия $version",
                    style = MaterialTheme.typography.bodySmall,
                    color = style.muted,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Восстановить из копии?") },
            text = { Text("Текущие журнал, вес, свои тренировки и настройки будут заменены данными из файла.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch { message = vm.restore(uri) ?: "Данные восстановлены." }
                }) { Text("Восстановить") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Отмена") } },
        )
    }
    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("ОК") } },
        )
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
                            RadioButton(selected = selected, colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary), onClick = {
                                vm.setSound(event, sound.key)
                                vm.preview(sound.key)
                            })
                            Text(sound.title, modifier = Modifier.weight(1f))
                            RoundIconButton(TempoIcons.Volume, "Прослушать", { vm.preview(sound.key) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = null }) { Text("Готово") } },
        )
    }
}
