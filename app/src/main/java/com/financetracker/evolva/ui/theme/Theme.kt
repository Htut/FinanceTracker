package com.financetracker.evolva.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Same palette as the web app's CSS custom properties, so the two look related. */
object FinanceColors {
    val Background = Color(0xFFFAFAFA)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFE5E5E5)
    val Text = Color(0xFF1A1A1A)
    val TextSoft = Color(0xFF6B6B6B)
    val Income = Color(0xFF1F8A5F)
    val Expense = Color(0xFFC0463B)
    val Savings = Color(0xFF2F6FB0)
    val Transfer = Color(0xFF7D5FA8)
    val Warn = Color(0xFFD98C3E)

    // Same 15-color rotation used for the expense-by-category donut.
    val CategoryPalette = listOf(
        Color(0xFFC0463B), Color(0xFFD98C3E), Color(0xFFD9C23E), Color(0xFF6FA85E),
        Color(0xFF2F6FB0), Color(0xFF7D5FA8), Color(0xFFA8567E), Color(0xFF5F9EA0),
        Color(0xFFB0752F), Color(0xFF8A8A8A), Color(0xFFE07A5F), Color(0xFF3D9970),
        Color(0xFF577590), Color(0xFF9C6644), Color(0xFF43AA8B)
    )
}

private val LightColors = lightColorScheme(
    primary = FinanceColors.Text,
    onPrimary = Color.White,
    secondary = FinanceColors.TextSoft,
    onSecondary = Color.White,
    background = FinanceColors.Background,
    onBackground = FinanceColors.Text,
    surface = FinanceColors.Surface,
    onSurface = FinanceColors.Text,
    surfaceVariant = FinanceColors.Background,
    outline = FinanceColors.Border,
    error = FinanceColors.Expense
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp),
    labelSmall = TextStyle(fontSize = 12.sp)
)

@Composable
fun FinanceTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
