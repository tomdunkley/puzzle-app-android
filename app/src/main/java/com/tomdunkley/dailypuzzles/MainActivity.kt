package com.tomdunkley.dailypuzzles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tomdunkley.dailypuzzles.navigation.DailyPuzzlesNavHost
import com.tomdunkley.dailypuzzles.ui.theme.DailyPuzzlesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyPuzzlesTheme {
                DailyPuzzlesNavHost()
            }
        }
    }
}
