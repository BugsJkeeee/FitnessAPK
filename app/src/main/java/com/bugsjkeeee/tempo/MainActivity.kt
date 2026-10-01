package com.bugsjkeeee.tempo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bugsjkeeee.tempo.ui.TempoNavHost
import com.bugsjkeeee.tempo.ui.theme.TempoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TempoTheme {
                TempoNavHost()
            }
        }
    }
}
