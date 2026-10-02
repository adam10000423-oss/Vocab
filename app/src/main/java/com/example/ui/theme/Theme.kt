package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF245FE5), onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EDFF), onPrimaryContainer = Color(0xFF163974),
    secondary = Color(0xFF586174), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E5EE), onSecondaryContainer = Color(0xFF202632),
    tertiary = Color(0xFF586174), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDF0F4), onTertiaryContainer = Color(0xFF202632),
    background = Color(0xFFF6F7F9), onBackground = Color(0xFF15171B),
    surface = Color.White, onSurface = Color(0xFF15171B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F5F8),
    surfaceContainer = Color(0xFFEDF0F4),
    surfaceContainerHigh = Color(0xFFE7EBF0),
    surfaceContainerHighest = Color(0xFFE1E5EC),
    surfaceTint = Color.Transparent,
    surfaceVariant = Color(0xFFEDF0F4), onSurfaceVariant = Color(0xFF5D6470),
    outline = Color(0xFF858C98), outlineVariant = Color(0xFFD8DCE3),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82AAFF), onPrimary = Color(0xFF092C6F),
    primaryContainer = Color(0xFF253554), onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFBCC4D6), onSecondary = Color(0xFF27303F),
    secondaryContainer = Color(0xFF343B49), onSecondaryContainer = Color(0xFFE0E5F0),
    tertiary = Color(0xFFBCC4D6), onTertiary = Color(0xFF27303F),
    tertiaryContainer = Color(0xFF262A32), onTertiaryContainer = Color(0xFFE0E5F0),
    background = Color(0xFF0F1115), onBackground = Color(0xFFF2F4F7),
    surface = Color(0xFF191C22), onSurface = Color(0xFFF2F4F7),
    surfaceContainerLowest = Color(0xFF0B0D11),
    surfaceContainerLow = Color(0xFF191C22),
    surfaceContainer = Color(0xFF22262E),
    surfaceContainerHigh = Color(0xFF292E37),
    surfaceContainerHighest = Color(0xFF323842),
    surfaceTint = Color.Transparent,
    surfaceVariant = Color(0xFF262A32), onSurfaceVariant = Color(0xFFC4CAD4),
    outline = Color(0xFF8E959F), outlineVariant = Color(0xFF3A3F48),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
