package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.R

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun presetForcesDark(preset: String): Boolean? = when (preset) {
    "BLACK", "DEEP_BLUE" -> true
    "WHITE" -> false
    else -> null
}

fun themeGradientColors(
    preset: String,
    customPrimary: String,
    customSecondary: String,
    darkTheme: Boolean,
    brightness: Float
): List<Color> {
    val (primary, secondary) = themeSeeds(preset, customPrimary, customSecondary)
    val base = if (darkTheme) Color(0xFF0F1216) else Color(0xFFF8FAFB)
    return listOf(
        adjustBrightness(blend(base, primary, if (darkTheme) 0.18f else 0.10f), brightness),
        adjustBrightness(blend(base, secondary, if (darkTheme) 0.20f else 0.12f), brightness)
    )
}

fun customGradientColors(
    start: String,
    end: String,
    darkTheme: Boolean,
    brightness: Float
): List<Color> {
    val fallbackStart = if (darkTheme) Color(0xFF152238) else Color(0xFFDFF5EC)
    val fallbackEnd = if (darkTheme) Color(0xFF24334A) else Color(0xFFDCEBFA)
    return listOf(
        adjustBrightness(parseColor(start, fallbackStart), brightness),
        adjustBrightness(parseColor(end, fallbackEnd), brightness)
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPreset: String = "GREEN",
    customPrimary: String = "#39796D",
    customSecondary: String = "#5B7482",
    backgroundBrightness: Float = 1f,
    backgroundOpacity: Float = 1f,
    gradientEnabled: Boolean = false,
    gradientStartColor: String = "#DFF5EC",
    gradientEndColor: String = "#DCEBFA",
    fontFamilyName: String = "DEFAULT",
    englishFontFamilyName: String = "DEFAULT",
    fontScale: Float = 1f,
    customTextColorEnabled: Boolean = false,
    customTextColor: String = "#202522",
    content: @Composable () -> Unit
) {
    val (rawPrimary, rawSecondary) =
        themeSeeds(colorPreset, customPrimary, customSecondary)
    val primary = if (darkTheme && rawPrimary.luminance() < 0.45f) {
        blend(rawPrimary, Color.White, 0.42f)
    } else rawPrimary
    val secondary = if (darkTheme && rawSecondary.luminance() < 0.42f) {
        blend(rawSecondary, Color.White, 0.38f)
    } else rawSecondary
    val backgrounds = if (gradientEnabled) {
        customGradientColors(
            gradientStartColor,
            gradientEndColor,
            darkTheme,
            backgroundBrightness
        )
    } else {
        themeGradientColors(
            colorPreset,
            customPrimary,
            customSecondary,
            darkTheme,
            backgroundBrightness
        )
    }
    val backgroundForContrast = blend(backgrounds[0], backgrounds[1], 0.5f)
    val background = if (gradientEnabled) {
        Color.Transparent
    } else {
        backgroundForContrast.copy(alpha = backgroundOpacity.coerceIn(0.55f, 1f))
    }
    val surface = blend(
        if (darkTheme) Color(0xFF171A20) else Color.White,
        primary,
        if (darkTheme) 0.08f else 0.035f
    ).copy(alpha = backgroundOpacity.coerceIn(0.72f, 1f))
    val automaticText = if (darkTheme) Color(0xFFF2F4F7) else Color(0xFF17191D)
    val requestedText = parseColor(customTextColor, automaticText)
    fun textOn(container: Color, fallback: Color): Color {
        val effectiveContainer = compositeOver(container, backgroundForContrast)
        return if (
            customTextColorEnabled && contrastRatio(requestedText, effectiveContainer) >= 4.5f
        ) requestedText else fallback
    }
    val onBackground = textOn(backgroundForContrast, automaticText)

    val scheme = if (darkTheme) {
        val primaryContainer = blend(primary, Color.Black, 0.48f)
        val secondaryContainer = blend(secondary, Color.Black, 0.45f)
        val tertiary = blend(primary, secondary, 0.5f)
        val surfaceVariant = blend(surface, Color.White, 0.09f)
        val error = Color(0xFFFFB4AB)
        val errorContainer = Color(0xFF93000A)
        darkColorScheme(
            primary = primary,
            onPrimary = textOn(primary, contrastOn(primary)),
            primaryContainer = primaryContainer,
            onPrimaryContainer = textOn(primaryContainer, Color(0xFFF4F7FA)),
            secondary = secondary,
            onSecondary = textOn(secondary, contrastOn(secondary)),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = textOn(secondaryContainer, Color(0xFFF4F7FA)),
            tertiary = tertiary,
            onTertiary = textOn(tertiary, contrastOn(tertiary)),
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = textOn(surface, automaticText),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textOn(surfaceVariant, Color(0xFFD2D7DE)),
            outline = Color(0xFF949BA5),
            error = error,
            onError = textOn(error, Color(0xFF690005)),
            errorContainer = errorContainer,
            onErrorContainer = textOn(errorContainer, Color(0xFFFFDAD6))
        )
    } else {
        val primaryContainer = blend(primary, Color.White, 0.78f)
        val secondaryContainer = blend(secondary, Color.White, 0.80f)
        val tertiary = blend(primary, secondary, 0.5f)
        val surfaceVariant = blend(surface, primary, 0.065f)
        val error = Color(0xFFB3261E)
        val errorContainer = Color(0xFFFFDAD6)
        lightColorScheme(
            primary = primary,
            onPrimary = textOn(primary, contrastOn(primary)),
            primaryContainer = primaryContainer,
            onPrimaryContainer = textOn(primaryContainer, Color(0xFF121417)),
            secondary = secondary,
            onSecondary = textOn(secondary, contrastOn(secondary)),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = textOn(secondaryContainer, Color(0xFF121417)),
            tertiary = tertiary,
            onTertiary = textOn(tertiary, contrastOn(tertiary)),
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = textOn(surface, automaticText),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textOn(surfaceVariant, Color(0xFF4D535B)),
            outline = Color(0xFF717780),
            error = error,
            onError = textOn(error, Color.White),
            errorContainer = errorContainer,
            onErrorContainer = textOn(errorContainer, Color(0xFF410002))
        )
    }
    val context = LocalContext.current
    val family = remember(fontFamilyName, englishFontFamilyName) {
        mixedLanguageFontFamily(context, fontFamilyName, englishFontFamilyName)
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = scaledTypography(family, fontScale.coerceIn(0.85f, 1.3f)),
        shapes = AppShapes,
        content = content
    )
}

private fun mixedLanguageFontFamily(
    context: Context,
    chineseFamilyName: String,
    englishFamilyName: String
): FontFamily {
    val chineseRes = when (chineseFamilyName) {
        "ROUNDED" -> R.font.huninn_regular
        "SERIF" -> R.font.noto_serif_tc
        "CURSIVE" -> R.font.iansui_regular
        "MONOSPACE" -> R.font.noto_sans_mono_cjk_tc_regular
        else -> R.font.noto_sans_tc
    }
    val englishRes = when (englishFamilyName) {
        "INTER" -> R.font.inter
        "NUNITO" -> R.font.nunito
        "PLAYFAIR" -> R.font.playfair_display
        "CAVEAT" -> R.font.caveat
        "JETBRAINS_MONO" -> R.font.jetbrains_mono
        else -> R.font.roboto
    }

    val typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val english = android.graphics.fonts.FontFamily.Builder(
            android.graphics.fonts.Font.Builder(context.resources, englishRes).build()
        ).build()
        val chinese = android.graphics.fonts.FontFamily.Builder(
            android.graphics.fonts.Font.Builder(context.resources, chineseRes).build()
        ).build()
        Typeface.CustomFallbackBuilder(english)
            .addCustomFallback(chinese)
            .setSystemFallback("sans-serif")
            .build()
    } else {
        // Custom per-script fallback is available from Android 10. Older
        // versions still honor the selected Chinese family and use its Latin
        // glyphs, so text remains complete and readable.
        ResourcesCompat.getFont(context, chineseRes) ?: Typeface.DEFAULT
    }
    return FontFamily(typeface)
}

private fun themeSeeds(
    preset: String,
    customPrimary: String,
    customSecondary: String
): Pair<Color, Color> = when (preset) {
    "BLACK" -> Color(0xFF20242A) to Color(0xFF626B78)
    "WHITE" -> Color(0xFF343A40) to Color(0xFF78838F)
    "DEEP_BLUE" -> Color(0xFF173F73) to Color(0xFF386A9F)
    "LIGHT_BLUE" -> Color(0xFF3F82B8) to Color(0xFF76B5D7)
    "TEAL" -> Color(0xFF137C78) to Color(0xFF3A968C)
    "PINK" -> Color(0xFFB34878) to Color(0xFFD2779C)
    "RED" -> Color(0xFFB43A3A) to Color(0xFFD16A55)
    "PURPLE" -> Color(0xFF7450A6) to Color(0xFF9B72BF)
    "ORANGE" -> Color(0xFFB96022) to Color(0xFFD7913E)
    "CUSTOM" -> parseColor(customPrimary, Color(0xFF39796D)) to
        parseColor(customSecondary, Color(0xFF5B7482))
    else -> Color(0xFF39796D) to Color(0xFF5B7482)
}

private fun parseColor(value: String, fallback: Color): Color = runCatching {
    val hex = value.trim().removePrefix("#")
    require(hex.length == 6)
    Color(("FF$hex").toLong(16))
}.getOrDefault(fallback)

private fun contrastOn(color: Color): Color =
    if (color.luminance() > 0.48f) Color(0xFF101214) else Color.White

private fun contrastRatio(first: Color, second: Color): Float {
    val light = maxOf(first.luminance(), second.luminance()) + 0.05f
    val dark = minOf(first.luminance(), second.luminance()) + 0.05f
    return light / dark
}

private fun compositeOver(foreground: Color, background: Color): Color {
    val foregroundAlpha = foreground.alpha.coerceIn(0f, 1f)
    val backgroundAlpha = background.alpha.coerceIn(0f, 1f)
    val outputAlpha = foregroundAlpha + backgroundAlpha * (1f - foregroundAlpha)
    if (outputAlpha <= 0f) return Color.Transparent
    return Color(
        red = (
            foreground.red * foregroundAlpha +
                background.red * backgroundAlpha * (1f - foregroundAlpha)
            ) / outputAlpha,
        green = (
            foreground.green * foregroundAlpha +
                background.green * backgroundAlpha * (1f - foregroundAlpha)
            ) / outputAlpha,
        blue = (
            foreground.blue * foregroundAlpha +
                background.blue * backgroundAlpha * (1f - foregroundAlpha)
            ) / outputAlpha,
        alpha = outputAlpha
    )
}

private fun blend(first: Color, second: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = first.red + (second.red - first.red) * t,
        green = first.green + (second.green - first.green) * t,
        blue = first.blue + (second.blue - first.blue) * t,
        alpha = first.alpha + (second.alpha - first.alpha) * t
    )
}

private fun adjustBrightness(color: Color, value: Float): Color = Color(
    red = (color.red * value).coerceIn(0f, 1f),
    green = (color.green * value).coerceIn(0f, 1f),
    blue = (color.blue * value).coerceIn(0f, 1f),
    alpha = color.alpha
)
