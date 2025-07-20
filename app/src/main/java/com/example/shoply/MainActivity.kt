package com.example.shoply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.shoply.screens.AppNavGraph
import com.example.shoply.ui.theme.ShoplyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoplyTheme {
                AppNavGraph()
            }
        }
    }
}