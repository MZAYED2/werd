package com.zekr.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zekr.widget.ui.screens.HomeScreen
import com.zekr.widget.ui.theme.ZekrWidgetTheme

class MainActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if launched from the Widget's long-press "Edit / Configure" menu
        intent?.extras?.let {
            appWidgetId = it.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        val isWidgetConfigMode = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID

        if (isWidgetConfigMode) {
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
        }

        setContent {
            ZekrWidgetTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen(
                    isWidgetConfigMode = isWidgetConfigMode,
                    onSaveAndClose = {
                        if (isWidgetConfigMode) {
                            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(Activity.RESULT_OK, resultValue)
                            finish()
                        }
                    }
                )
            }
        }
    }
}
