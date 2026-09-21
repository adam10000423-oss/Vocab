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

fun scaledTypography(family: FontFamily, scale: Float): Typography =
  Typography.copy(
    headlineSmall = Typography.headlineSmall.copy(
      fontFamily = family,
      fontSize = Typography.headlineSmall.fontSize * scale,
      lineHeight = Typography.headlineSmall.lineHeight * scale
    ),
    titleLarge = Typography.titleLarge.copy(fontFamily = family, fontSize = Typography.titleLarge.fontSize * scale),
    titleMedium = Typography.titleMedium.copy(fontFamily = family, fontSize = Typography.titleMedium.fontSize * scale),
    titleSmall = Typography.titleSmall.copy(fontFamily = family, fontSize = Typography.titleSmall.fontSize * scale),
    bodyLarge = Typography.bodyLarge.copy(fontFamily = family, fontSize = Typography.bodyLarge.fontSize * scale),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = family, fontSize = Typography.bodyMedium.fontSize * scale),
    bodySmall = Typography.bodySmall.copy(fontFamily = family, fontSize = Typography.bodySmall.fontSize * scale),
    labelLarge = Typography.labelLarge.copy(fontFamily = family, fontSize = Typography.labelLarge.fontSize * scale),
    labelMedium = Typography.labelMedium.copy(fontFamily = family, fontSize = Typography.labelMedium.fontSize * scale),
    labelSmall = Typography.labelSmall.copy(fontFamily = family, fontSize = Typography.labelSmall.fontSize * scale)
  )
