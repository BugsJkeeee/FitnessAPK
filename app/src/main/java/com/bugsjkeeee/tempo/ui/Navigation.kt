package com.bugsjkeeee.tempo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bugsjkeeee.tempo.ui.components.RoundIconButton
import com.bugsjkeeee.tempo.ui.components.ScreenHeader
import com.bugsjkeeee.tempo.ui.icons.TempoIcons
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.ui.base.BaseScreen
import com.bugsjkeeee.tempo.ui.base.ExerciseDetailScreen
import com.bugsjkeeee.tempo.ui.base.ExercisePickerScreen
import com.bugsjkeeee.tempo.ui.base.WorkoutDetailScreen
import com.bugsjkeeee.tempo.ui.base.WorkoutEditScreen
import com.bugsjkeeee.tempo.ui.base.WorkoutPickerScreen
import com.bugsjkeeee.tempo.ui.execute.ExecuteScreen
import com.bugsjkeeee.tempo.ui.journal.ComplexRecordScreen
import com.bugsjkeeee.tempo.ui.journal.EntryScreen
import com.bugsjkeeee.tempo.ui.journal.ExerciseRecordScreen
import com.bugsjkeeee.tempo.ui.journal.JournalEditScreen
import com.bugsjkeeee.tempo.ui.journal.JournalScreen
import com.bugsjkeeee.tempo.ui.random.RandomScreen
import com.bugsjkeeee.tempo.ui.settings.SettingsScreen
import com.bugsjkeeee.tempo.ui.timer.TimerRunScreen
import com.bugsjkeeee.tempo.ui.timer.TimerSetupScreen
import com.bugsjkeeee.tempo.ui.weight.WeightScreen

object Routes {
    const val RANDOM = "random"
    const val TIMER = "timer"
    const val BASE = "base"
    const val JOURNAL = "journal"
    const val WEIGHT = "weight"
    const val TIMER_RUN = "timer_run"
    const val SETTINGS = "settings"
    const val WORKOUT = "workout/{id}"
    const val WORKOUT_EDIT = "workout_edit?id={id}"
    const val EXERCISE = "exercise/{id}"
    const val EXERCISE_PICK = "exercise_pick"
    const val WORKOUT_PICK = "workout_pick"
    const val EXECUTE = "execute/{id}"
    const val ENTRY = "entry/{id}"
    const val ENTRY_EDIT = "entry_edit?id={id}&workout={workout}&timer={timer}"
    const val RECORD_EXERCISE = "record_exercise/{id}"
    const val RECORD_COMPLEX = "record_complex/{id}"

    fun workout(id: String) = "workout/$id"
    fun workoutEdit(id: String?) = if (id == null) "workout_edit" else "workout_edit?id=$id"
    fun exercise(id: String) = "exercise/$id"
    fun execute(id: String) = "execute/$id"
    fun entry(id: Long) = "entry/$id"
    fun entryEdit(id: Long = 0, workout: String? = null, timer: Boolean = false) =
        "entry_edit?id=$id&timer=$timer" + (workout?.let { "&workout=$it" } ?: "")
    fun recordExercise(id: String) = "record_exercise/$id"
    fun recordComplex(id: String) = "record_complex/$id"

    /** Ключи, под которыми экраны выбора возвращают результат. */
    const val PICKED_EXERCISE = "picked_exercise"
    const val PICKED_WORKOUT = "picked_workout"
}

private data class Tab(val route: String, val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.RANDOM, "Рандом", TempoIcons.Random),
    Tab(Routes.TIMER, "Таймер", TempoIcons.Timer),
    Tab(Routes.BASE, "База", TempoIcons.Dumbbell),
    Tab(Routes.JOURNAL, "Журнал", TempoIcons.Journal),
    Tab(Routes.WEIGHT, "Вес", TempoIcons.Scale),
)

/**
 * Нижняя панель: неактивные вкладки — только иконки, активная — оранжевая «таблетка» с подписью.
 * При переключении подпись прежней вкладки сворачивается, новой — разворачивается.
 */
@Composable
private fun TabBar(current: String?, onSelect: (String) -> Unit) {
    val style = LocalTempoStyle.current
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .background(style.tile.copy(alpha = 0.97f))
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(style.outline))
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { t ->
                val selected = t.route == current
                val bg by animateColorAsState(if (selected) accent else Color.Transparent, tween(250), label = "tabBg")
                val tint by animateColorAsState(if (selected) Color.White else style.muted, tween(250), label = "tabTint")
                Row(
                    Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(t.route) }
                        .animateContentSize(tween(250))
                        .padding(horizontal = if (selected) 16.dp else 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(t.icon, contentDescription = t.title, tint = tint, modifier = Modifier.size(22.dp))
                    AnimatedVisibility(
                        visible = selected,
                        enter = expandHorizontally(tween(250)) + fadeIn(tween(250)),
                        exit = shrinkHorizontally(tween(250)) + fadeOut(tween(150)),
                    ) {
                        Text(
                            t.title,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Значение, возвращённое экраном выбора, и сброс после обработки. */
@Composable
private fun NavBackStackEntry.picked(key: String): String? {
    val value by savedStateHandle.getStateFlow<String?>(key, null).collectAsStateWithLifecycle()
    return value
}

@Composable
fun TempoNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tab = tabs.firstOrNull { it.route == route }
    val controller = (LocalContext.current.applicationContext as TempoApp).timerController
    val go: (String) -> Unit = { navController.navigate(it) }
    val back: () -> Unit = { navController.popBackStack() }
    // Счётчик возвратов на вкладку «Рандом»: по ТЗ фильтры сбрасываются при каждом открытии раздела.
    var randomReset by rememberSaveable { mutableIntStateOf(0) }

    // Если таймер уже идёт (например, приложение открыли из уведомления), сразу показываем его.
    LaunchedEffect(Unit) {
        if (controller.state.value != null) navController.navigate(Routes.TIMER_RUN)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (tab != null) {
                ScreenHeader(tab.title, Modifier.statusBarsPadding()) {
                    RoundIconButton(TempoIcons.Settings, "Настройки", onClick = { go(Routes.SETTINGS) })
                }
            }
        },
        bottomBar = {
            if (tab != null) {
                TabBar(route) { r ->
                    if (r == Routes.RANDOM && route != Routes.RANDOM) randomReset++
                    navController.navigate(r) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Routes.RANDOM) {
            composable(Routes.RANDOM) {
                RandomScreen(padding, resetKey = randomReset, onNavigate = go, onExercise = { go(Routes.exercise(it)) })
            }
            composable(Routes.TIMER) {
                TimerSetupScreen(
                    onOpenRunning = { go(Routes.TIMER_RUN) },
                    onStarted = { go(Routes.TIMER_RUN) },
                    contentPadding = padding,
                )
            }
            composable(Routes.BASE) {
                BaseScreen(
                    padding,
                    onWorkout = { go(Routes.workout(it)) },
                    onExercise = { go(Routes.exercise(it)) },
                    onNewWorkout = { go(Routes.workoutEdit(null)) },
                )
            }
            composable(Routes.JOURNAL) {
                JournalScreen(
                    padding,
                    onEntry = { go(Routes.entry(it)) },
                    onAdd = { go(Routes.entryEdit()) },
                    onExerciseRecord = { go(Routes.recordExercise(it)) },
                    onComplexRecord = { go(Routes.recordComplex(it)) },
                )
            }
            composable(Routes.WEIGHT) { WeightScreen(padding) }

            composable(Routes.TIMER_RUN) {
                TimerRunScreen(
                    onClose = { navController.popBackStack(Routes.TIMER_RUN, inclusive = true) },
                    onSaveToJournal = { go(Routes.entryEdit(timer = true)) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
            composable(Routes.WORKOUT, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                WorkoutDetailScreen(
                    id = e.arguments?.getString("id").orEmpty(),
                    onBack = back,
                    onNavigate = go,
                    onExercise = { go(Routes.exercise(it)) },
                    onEdit = { go(Routes.workoutEdit(it)) },
                )
            }
            composable(
                Routes.WORKOUT_EDIT,
                listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { e ->
                WorkoutEditScreen(
                    id = e.arguments?.getString("id"),
                    pickedExercise = e.picked(Routes.PICKED_EXERCISE),
                    onPickedConsumed = { e.savedStateHandle[Routes.PICKED_EXERCISE] = null },
                    onPickExercise = { go(Routes.EXERCISE_PICK) },
                    onDone = back,
                )
            }
            composable(Routes.EXERCISE, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                ExerciseDetailScreen(e.arguments?.getString("id").orEmpty(), onBack = back)
            }
            composable(Routes.EXERCISE_PICK) {
                ExercisePickerScreen(
                    onPicked = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(Routes.PICKED_EXERCISE, id)
                        navController.popBackStack()
                    },
                    onBack = back,
                )
            }
            composable(Routes.WORKOUT_PICK) {
                WorkoutPickerScreen(
                    onPicked = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(Routes.PICKED_WORKOUT, id)
                        navController.popBackStack()
                    },
                    onBack = back,
                )
            }
            composable(Routes.EXECUTE, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                ExecuteScreen(e.arguments?.getString("id").orEmpty(), onClose = back)
            }
            composable(Routes.ENTRY, listOf(navArgument("id") { type = NavType.LongType })) { e ->
                EntryScreen(e.arguments?.getLong("id") ?: 0L, onBack = back, onEdit = { go(Routes.entryEdit(it)) })
            }
            composable(
                Routes.ENTRY_EDIT,
                listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("workout") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("timer") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { e ->
                JournalEditScreen(
                    entryId = e.arguments?.getLong("id") ?: 0L,
                    workoutId = e.arguments?.getString("workout"),
                    fromTimer = e.arguments?.getBoolean("timer") ?: false,
                    pickedExercise = e.picked(Routes.PICKED_EXERCISE),
                    pickedWorkout = e.picked(Routes.PICKED_WORKOUT),
                    onPickedConsumed = {
                        e.savedStateHandle[Routes.PICKED_EXERCISE] = null
                        e.savedStateHandle[Routes.PICKED_WORKOUT] = null
                    },
                    onPickExercise = { go(Routes.EXERCISE_PICK) },
                    onPickWorkout = { go(Routes.WORKOUT_PICK) },
                    onDone = back,
                )
            }
            composable(Routes.RECORD_EXERCISE, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                ExerciseRecordScreen(e.arguments?.getString("id").orEmpty(), onBack = back)
            }
            composable(Routes.RECORD_COMPLEX, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                ComplexRecordScreen(e.arguments?.getString("id").orEmpty(), onBack = back, onEntry = { go(Routes.entry(it)) })
            }
        }
    }
}
