package com.zekr.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.view.WindowCompat
import com.zekr.widget.ui.theme.rememberWerdAppearance
import com.zekr.widget.ui.theme.rememberWerdDarkTheme
import com.zekr.widget.ui.screens.HomeScreen
import com.zekr.widget.ui.theme.ZekrWidgetTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAV_TAB = "extra_nav_tab"
        const val NAV_GUIDED_QURAN = 6
        const val NAV_MUSHAF = 7
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val targetTab = mutableIntStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if launched from the Widget's long-press "Edit / Configure" menu or addition
        intent?.extras?.let {
            appWidgetId = it.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val tab = it.getInt(EXTRA_NAV_TAB, -1)
            if (tab >= 0 && savedInstanceState == null) {
                targetTab.intValue = tab
            }
        }

        val isWidgetConfigMode = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID

        if (isWidgetConfigMode) {
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
        }

        setContent {
            val appearance = rememberWerdAppearance().value
            val darkTheme = rememberWerdDarkTheme(appearance)
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            ZekrWidgetTheme(darkTheme = darkTheme, accent = appearance.accent) {
                HomeScreen(
                    isWidgetConfigMode = isWidgetConfigMode,
                    widgetId = appWidgetId,
                    initialTab = targetTab.intValue,
                    onInitialTabHandled = { targetTab.intValue = -1 },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val tab = intent.getIntExtra(EXTRA_NAV_TAB, -1)
        if (tab >= 0) {
            targetTab.intValue = tab
        }
    }
}
