package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared responsive breakpoints used by every Android screen. */
@Immutable
data class ResponsiveLayout(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val fontScale: Float,
    val isLandscape: Boolean,
    val isSmallWidth: Boolean,
    val isShortHeight: Boolean,
    val isLargeText: Boolean
) {
    val isConstrained: Boolean
        get() = isSmallWidth || isShortHeight || isLandscape || isLargeText

    val horizontalPadding: Dp
        get() = if (isSmallWidth) 12.dp else 20.dp

    val dialogContentMaxHeight: Dp
        get() = (screenHeightDp * if (isLandscape) 0.62f else 0.7f).dp

    val flashcardHeight: Dp
        get() = when {
            isLandscape -> 280.dp
            screenHeightDp < 640 -> 350.dp
            isLargeText -> 420.dp
            else -> 440.dp
        }
}

@Composable
fun rememberResponsiveLayout(): ResponsiveLayout {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    return remember(width, height, fontScale, landscape) {
        ResponsiveLayout(
            screenWidthDp = width,
            screenHeightDp = height,
            fontScale = fontScale,
            isLandscape = landscape,
            isSmallWidth = width < 360,
            isShortHeight = height < 600,
            isLargeText = fontScale >= 1.3f
        )
    }
}
