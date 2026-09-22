package com.example

import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.scaledTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyThemeTest {
    @Test
    fun selectedFontAndScaleApplyToEveryMaterialTextRole() {
        val normal = scaledTypography(FontFamily.Default, 1f)
        val themed = scaledTypography(FontFamily.Monospace, 1.2f)
        val styles = listOf(
            themed.displayLarge,
            themed.displayMedium,
            themed.displaySmall,
            themed.headlineLarge,
            themed.headlineMedium,
            themed.headlineSmall,
            themed.titleLarge,
            themed.titleMedium,
            themed.titleSmall,
            themed.bodyLarge,
            themed.bodyMedium,
            themed.bodySmall,
            themed.labelLarge,
            themed.labelMedium,
            themed.labelSmall
        )

        styles.forEach { assertEquals(FontFamily.Monospace, it.fontFamily) }
        assertTrue(themed.headlineLarge.fontSize > normal.headlineLarge.fontSize)
        assertTrue(themed.bodyMedium.lineHeight > normal.bodyMedium.lineHeight)
    }
}
