package com.bugsjkeeee.tempo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bugsjkeeee.tempo.TempoApp
import com.bugsjkeeee.tempo.ui.components.ComingSoon
import com.bugsjkeeee.tempo.ui.settings.SettingsScreen
import com.bugsjkeeee.tempo.ui.timer.TimerRunScreen
import com.bugsjkeeee.tempo.ui.timer.TimerSetupScreen

object Routes {
    const val RANDOM = "random"
    const val TIMER = "timer"
    const val BASE = "base"
    const val JOURNAL = "journal"
    const val WEIGHT = "weight"
    const val TIMER_RUN = "timer_run"
    const val SETTINGS = "settings"
}

private data class Tab(val route: String, val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.RANDOM, "Рандом", Icons.Filled.Casino),
    Tab(Routes.TIMER, "Таймер", Icons.Filled.Timer),
    Tab(Routes.BASE, "База", Icons.Filled.FitnessCenter),
    Tab(Routes.JOURNAL, "Журнал", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.WEIGHT, "Вес", Icons.Filled.MonitorWeight),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tab = tabs.firstOrNull { it.route == route }
    val controller = (LocalContext.current.applicationContext as TempoApp).timerController

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
                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                        }
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
        NavHost(navController, startDestination = Routes.TIMER) {
            composable(Routes.RANDOM) {
                ComingSoon(
                    "Рандомайзер", 2,
                    "Случайная тренировка из базы с фильтрами по типу, мышцам, длительности, формату, сложности и оборудованию.",
                    Modifier.padding(padding),
                )
            }
            composable(Routes.TIMER) {
                TimerSetupScreen(
                    onOpenRunning = { navController.navigate(Routes.TIMER_RUN) },
                    onStarted = { navController.navigate(Routes.TIMER_RUN) },
                    contentPadding = padding,
                )
            }
            composable(Routes.BASE) {
                ComingSoon("База", 2, "Около 150 тренировок, справочник упражнений с фото и техникой, избранное и свои тренировки.", Modifier.padding(padding))
            }
            composable(Routes.JOURNAL) {
                ComingSoon("Журнал", 3, "Список и календарь тренировок, ручное добавление, личные рекорды.", Modifier.padding(padding))
            }
            composable(Routes.WEIGHT) {
                ComingSoon("Вес", 4, "Ввод веса, график с периодом, целью и трендом.", Modifier.padding(padding))
            }
            composable(Routes.TIMER_RUN) {
                TimerRunScreen(onClose = { navController.popBackStack(Routes.TIMER_RUN, inclusive = true) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
