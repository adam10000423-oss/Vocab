package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.VocabApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.presetForcesDark
import com.example.ui.theme.themeGradientColors
import com.example.ui.theme.customGradientColors
import com.example.viewmodel.VocabularyViewModel

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
            val darkTheme = presetForcesDark(settings.themeColorPreset) ?: isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            val gradientColors = if (settings.gradientEnabled) {
                customGradientColors(
                    start = settings.gradientStartColor,
                    end = settings.gradientEndColor,
                    darkTheme = darkTheme,
                    brightness = settings.backgroundBrightness
                )
            } else {
                themeGradientColors(
                    preset = settings.themeColorPreset,
                    customPrimary = settings.customPrimaryColor,
                    customSecondary = settings.customSecondaryColor,
                    darkTheme = darkTheme,
                    brightness = settings.backgroundBrightness
                )
            }
            MyApplicationTheme(
                darkTheme = darkTheme,
                colorPreset = settings.themeColorPreset,
                customPrimary = settings.customPrimaryColor,
                customSecondary = settings.customSecondaryColor,
                backgroundBrightness = settings.backgroundBrightness,
                backgroundOpacity = settings.backgroundOpacity,
                gradientEnabled = settings.gradientEnabled,
                gradientStartColor = settings.gradientStartColor,
                gradientEndColor = settings.gradientEndColor,
                fontFamilyName = settings.fontFamily,
                fontScale = settings.fontScale,
                customTextColorEnabled = settings.customTextColorEnabled,
                customTextColor = settings.customTextColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (settings.gradientEnabled) {
                                Brush.linearGradient(gradientColors)
                            } else {
                                Brush.linearGradient(
                                    listOf(gradientColors.first(), gradientColors.first())
                                )
                            }
                        )
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
