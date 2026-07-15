package com.cfquotamonitor.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CfOrange = Color(0xFFF48120)
val CfOrangeDark = Color(0xFFC95D06)
val SafeGreen = Color(0xFF159567)
val WarningAmber = Color(0xFFE89B16)
val DangerRed = Color(0xFFE04444)

private val LightColors = lightColorScheme(
    primary = CfOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1C9),
    onPrimaryContainer = Color(0xFF3A1600),
    secondary = Color(0xFF52606D),
    background = Color(0xFFF7F7F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F2),
    outline = Color(0xFFD4D4D8),
    error = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA45B),
    onPrimary = Color(0xFF351000),
    primaryContainer = Color(0xFF6B2C00),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFBBC7D2),
    background = Color(0xFF101114),
    surface = Color(0xFF191A1E),
    surfaceVariant = Color(0xFF24262B),
    outline = Color(0xFF43464D),
    error = Color(0xFFFF6B6B),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
)

@Composable
fun CfQuotaTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
