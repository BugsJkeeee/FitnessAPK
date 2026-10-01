package com.bugsjkeeee.tempo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
    Tab(Routes.RANDOM, "Рандом", Icons.Filled.Casino),
    Tab(Routes.TIMER, "Таймер", Icons.Filled.Timer),
    Tab(Routes.BASE, "База", Icons.Filled.FitnessCenter),
    Tab(Routes.JOURNAL, "Журнал", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.WEIGHT, "Вес", Icons.Filled.MonitorWeight),
)

/** Значение, возвращённое экраном выбора, и сброс после обработки. */
@Composable
private fun NavBackStackEntry.picked(key: String): String? {
    val value by savedStateHandle.getStateFlow<String?>(key, null).collectAsStateWithLifecycle()
    return value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tab = tabs.firstOrNull { it.route == route }
    val controller = (LocalContext.current.applicationContext as TempoApp).timerController
    val go: (String) -> Unit = { navController.navigate(it) }
    val back: () -> Unit = { navController.popBackStack() }

    // Если таймер уже идёт (например, приложение открыли из уведомления), сразу показываем его.
    LaunchedEffect(Unit) {
        if (controller.state.value != null) navController.navigate(Routes.TIMER_RUN)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (tab != null) {
                TopAppBar(
                    title = { Text(tab.title) },
                    actions = {
                        IconButton(onClick = { go(Routes.SETTINGS) }) { Icon(Icons.Filled.Settings, contentDescription = "Настройки") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        bottomBar = {
            if (tab != null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = t.route == route,
                            onClick = {
                                navController.navigate(t.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Routes.RANDOM) {
            composable(Routes.RANDOM) {
                RandomScreen(padding, onNavigate = go, onExercise = { go(Routes.exercise(it)) })
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
