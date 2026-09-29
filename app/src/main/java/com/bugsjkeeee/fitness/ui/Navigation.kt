package com.bugsjkeeee.fitness.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bugsjkeeee.fitness.ui.screens.ActiveWorkoutScreen
import com.bugsjkeeee.fitness.ui.screens.BodyWeightScreen
import com.bugsjkeeee.fitness.ui.screens.ExercisePickerScreen
import com.bugsjkeeee.fitness.ui.screens.HistoryScreen
import com.bugsjkeeee.fitness.ui.screens.HomeScreen
import com.bugsjkeeee.fitness.ui.screens.ProgressScreen
import com.bugsjkeeee.fitness.ui.screens.TemplateEditScreen
import com.bugsjkeeee.fitness.ui.screens.WorkoutDetailScreen

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val PROGRESS = "progress"
    const val WEIGHT = "weight"
    const val WORKOUT = "workout"
    const val PICKER = "picker?pick={pick}"
    const val TEMPLATE = "template/{id}"
    const val WORKOUT_DETAIL = "workout_detail/{id}"

    fun picker(pick: Boolean) = "picker?pick=$pick"
    fun template(id: Long) = "template/$id"
    fun workoutDetail(id: Long) = "workout_detail/$id"

    /** Ключ, под которым экран выбора возвращает id упражнения предыдущему экрану. */
    const val PICKED_EXERCISE = "picked_exercise"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Тренировка", Icons.Filled.FitnessCenter),
    Tab(Routes.HISTORY, "История", Icons.Filled.History),
    Tab(Routes.PROGRESS, "Прогресс", Icons.AutoMirrored.Filled.ShowChart),
    Tab(Routes.WEIGHT, "Вес тела", Icons.Filled.MonitorWeight),
)

@Composable
fun FitnessNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    // Отступы системных панелей обрабатывают сами экраны; здесь учитывается только нижняя навигация.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenWorkout = { navController.navigate(Routes.WORKOUT) },
                    onEditTemplate = { navController.navigate(Routes.template(it)) },
                    onOpenExercises = { navController.navigate(Routes.picker(pick = false)) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpenWorkout = { navController.navigate(Routes.workoutDetail(it)) })
            }
            composable(Routes.PROGRESS) { ProgressScreen() }
            composable(Routes.WEIGHT) { BodyWeightScreen() }

            composable(Routes.WORKOUT) { entry ->
                val picked by entry.savedStateHandle
                    .getStateFlow<Long?>(Routes.PICKED_EXERCISE, null)
                    .collectAsStateWithLifecycle()
                ActiveWorkoutScreen(
                    pickedExerciseId = picked,
                    onPickedConsumed = { entry.savedStateHandle[Routes.PICKED_EXERCISE] = null },
                    onAddExercise = { navController.navigate(Routes.picker(pick = true)) },
                    onClose = { navController.popBackStack() },
                )
            }
            composable(
                Routes.PICKER,
                arguments = listOf(navArgument("pick") { type = NavType.BoolType; defaultValue = true }),
            ) { entry ->
                val pick = entry.arguments?.getBoolean("pick") ?: true
                ExercisePickerScreen(
                    pickMode = pick,
                    onPicked = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(Routes.PICKED_EXERCISE, id)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.TEMPLATE,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val picked by entry.savedStateHandle
                    .getStateFlow<Long?>(Routes.PICKED_EXERCISE, null)
                    .collectAsStateWithLifecycle()
                TemplateEditScreen(
                    templateId = entry.arguments?.getLong("id") ?: 0L,
                    pickedExerciseId = picked,
                    onPickedConsumed = { entry.savedStateHandle[Routes.PICKED_EXERCISE] = null },
                    onAddExercise = { navController.navigate(Routes.picker(pick = true)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.WORKOUT_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                WorkoutDetailScreen(
                    workoutId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
