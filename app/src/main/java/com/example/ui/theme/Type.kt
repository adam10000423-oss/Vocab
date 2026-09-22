package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography =
  Typography(
    headlineSmall =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 31.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
      ),
    labelSmall = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 15.sp,
      letterSpacing = 0.1.sp,
    )
  )

fun scaledTypography(family: FontFamily, scale: Float): Typography {
  val safeScale = scale.coerceIn(0.85f, 1.3f)
  fun TextStyle.applyAppFont(): TextStyle = copy(
    fontFamily = family,
    fontSize = fontSize * safeScale,
    lineHeight = lineHeight * safeScale
  )
  return Typography.copy(
    displayLarge = Typography.displayLarge.applyAppFont(),
    displayMedium = Typography.displayMedium.applyAppFont(),
    displaySmall = Typography.displaySmall.applyAppFont(),
    headlineLarge = Typography.headlineLarge.applyAppFont(),
    headlineMedium = Typography.headlineMedium.applyAppFont(),
    headlineSmall = Typography.headlineSmall.applyAppFont(),
    titleLarge = Typography.titleLarge.applyAppFont(),
    titleMedium = Typography.titleMedium.applyAppFont(),
    titleSmall = Typography.titleSmall.applyAppFont(),
    bodyLarge = Typography.bodyLarge.applyAppFont(),
    bodyMedium = Typography.bodyMedium.applyAppFont(),
    bodySmall = Typography.bodySmall.applyAppFont(),
    labelLarge = Typography.labelLarge.applyAppFont(),
    labelMedium = Typography.labelMedium.applyAppFont(),
    labelSmall = Typography.labelSmall.applyAppFont()
  )
}
