package com.tecsup.galvanguerrero.eventplanner_azanero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tecsup.galvanguerrero.eventplanner_azanero.navigation.NavigationGraph
import com.tecsup.galvanguerrero.eventplanner_azanero.ui.theme.EventPlanner_azaneroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventPlanner_azaneroTheme {
                NavigationGraph()
            }
        }
    }
}