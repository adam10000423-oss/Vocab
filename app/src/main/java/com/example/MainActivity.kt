package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.VocabApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VocabularyViewModel
import com.example.util.AutoPlayOverlayService

class MainActivity : ComponentActivity() {
    private var widgetAction by mutableStateOf<String?>(null)
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetAction = intent.getStringExtra(EXTRA_WIDGET_ACTION)
        sharedText = intent.sharedEnglishText()
        enableEdgeToEdge()
        setContent {
            val appViewModel: VocabularyViewModel = viewModel()
            val settings = appViewModel.settings.collectAsStateWithLifecycle().value
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDarkTheme
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // In landscape, gesture/navigation controls can live on either
                        // side of the display. Protect every screen at the app root.
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    VocabApp(
                        viewModel = appViewModel,
                        widgetAction = widgetAction,
                        onWidgetActionConsumed = { widgetAction = null },
                        sharedText = sharedText,
                        onSharedTextConsumed = { sharedText = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetAction = intent.getStringExtra(EXTRA_WIDGET_ACTION)
        sharedText = intent.sharedEnglishText()
    }

    override fun onStart() {
        super.onStart()
        AutoPlayOverlayService.setAppVisible(true)
    }

    override fun onStop() {
        AutoPlayOverlayService.setAppVisible(false)
        super.onStop()
    }

    private fun Intent.sharedEnglishText(): String? =
        takeIf { action == Intent.ACTION_SEND && type?.startsWith("text/") == true }
            ?.getCharSequenceExtra(Intent.EXTRA_TEXT)
            ?.toString()
            ?.trim()
            ?.take(20_000)
            ?.takeIf { it.isNotBlank() }

    companion object {
        const val EXTRA_WIDGET_ACTION = "widget_action"
        const val WIDGET_ACTION_REVIEW = "start_review"
    }
}
