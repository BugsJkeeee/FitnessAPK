package com.bugsjkeeee.fitness.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bugsjkeeee.fitness.FitnessApp
import com.bugsjkeeee.fitness.data.FitnessRepository

/** Создаёт ViewModel экрана, передавая ей репозиторий из контейнера приложения. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (FitnessRepository) -> VM): VM {
    val app = LocalContext.current.applicationContext as FitnessApp
    return viewModel(factory = viewModelFactory { initializer { create(app.repository) } })
}

/** Экраны вкладок: нижний системный отступ уже учтён панелью навигации. */
val TabScreenInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
