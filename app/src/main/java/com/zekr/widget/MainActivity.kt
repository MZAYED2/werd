package com.zekr.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zekr.widget.ui.screens.HomeScreen
import com.zekr.widget.ui.theme.ZekrWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZekrWidgetTheme {
                HomeScreen()
            }
        }
    }
}
