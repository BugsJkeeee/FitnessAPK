package com.bugsjkeeee.tempo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bugsjkeeee.tempo.TempoApp

/** Создаёт ViewModel экрана с доступом к контейнеру приложения. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (TempoApp) -> VM): VM {
    val app = LocalContext.current.applicationContext as TempoApp
    return viewModel(factory = viewModelFactory { initializer { create(app) } })
}
