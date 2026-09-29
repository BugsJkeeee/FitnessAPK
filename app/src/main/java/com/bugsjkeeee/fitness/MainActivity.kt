package com.bugsjkeeee.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bugsjkeeee.fitness.ui.FitnessNavHost
import com.bugsjkeeee.fitness.ui.theme.FitnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitnessTheme {
                FitnessNavHost()
            }
        }
    }
}
