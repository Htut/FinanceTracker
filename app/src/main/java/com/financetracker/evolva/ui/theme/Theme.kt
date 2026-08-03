package com.financetracker.evolva.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.R

data class AppColorPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val text: Color,
    val textSoft: Color,
    val income: Color,
    val expense: Color,
    val savings: Color,
    val transfer: Color,
    val warn: Color,
    val header: Color,
    val onHeader: Color,
    val bandEven: Color,
    val bandOdd: Color,
    val accent: Color,
    val isDark: Boolean,
    val categoryPalette: List<Color>
)

enum class AppThemeOption(val id: String, val labelRes: Int, val subtitleRes: Int) {
    CLASSIC("classic", R.string.theme_classic, R.string.theme_classic_sub),
    FOREST("forest", R.string.theme_forest, R.string.theme_forest_sub),
    OCEAN("ocean", R.string.theme_ocean, R.string.theme_ocean_sub),
    MIDNIGHT("midnight", R.string.theme_midnight, R.string.theme_midnight_sub),
    AMBER("amber", R.string.theme_amber, R.string.theme_amber_sub),
    SLATE("slate", R.string.theme_slate, R.string.theme_slate_sub);

    companion object {
        fun fromId(id: String?): AppThemeOption =
            entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

private val DefaultCategories = listOf(
    Color(0xFFC0463B), Color(0xFFD98C3E), Color(0xFFD9C23E), Color(0xFF6FA85E),
    Color(0xFF2F6FB0), Color(0xFF7D5FA8), Color(0xFFA8567E), Color(0xFF5F9EA0),
    Color(0xFFB0752F), Color(0xFF8A8A8A), Color(0xFFE07A5F), Color(0xFF3D9970),
    Color(0xFF577590), Color(0xFF9C6644), Color(0xFF43AA8B)
)

fun AppThemeOption.palette(): AppColorPalette = when (this) {
    AppThemeOption.CLASSIC -> AppColorPalette(
        background = Color(0xFFFAFAFA),
        surface = Color(0xFFFFFFFF),
        border = Color(0xFFE5E5E5),
        text = Color(0xFF1A1A1A),
        textSoft = Color(0xFF6B6B6B),
        income = Color(0xFF1F8A5F),
        expense = Color(0xFFC0463B),
        savings = Color(0xFF2F6FB0),
        transfer = Color(0xFF7D5FA8),
        warn = Color(0xFFD98C3E),
        header = Color(0xFF0B2A4A),
        onHeader = Color(0xFFF5F8FC),
        bandEven = Color(0xFFFFFFFF),
        bandOdd = Color(0xFFF0F2F5),
        accent = Color(0xFF2F6FB0),
        isDark = false,
        categoryPalette = DefaultCategories
    )
    AppThemeOption.FOREST -> AppColorPalette(
        background = Color(0xFFF3F7F4),
        surface = Color(0xFFFFFFFF),
        border = Color(0xFFD5E3D9),
        text = Color(0xFF0F2A24),
        textSoft = Color(0xFF5A7368),
        income = Color(0xFF1F8A5F),
        expense = Color(0xFFC0463B),
        savings = Color(0xFF1B6B4A),
        transfer = Color(0xFF2F6FB0),
        warn = Color(0xFFD98C3E),
        header = Color(0xFF1B4D3E),
        onHeader = Color(0xFFF3FBF6),
        bandEven = Color(0xFFFFFFFF),
        bandOdd = Color(0xFFE8F2EB),
        accent = Color(0xFF2ECC71),
        isDark = false,
        categoryPalette = DefaultCategories
    )
    AppThemeOption.OCEAN -> AppColorPalette(
        background = Color(0xFFF2F7FB),
        surface = Color(0xFFFFFFFF),
        border = Color(0xFFD3E2EE),
        text = Color(0xFF12263A),
        textSoft = Color(0xFF5E7388),
        income = Color(0xFF1F8A5F),
        expense = Color(0xFFC0463B),
        savings = Color(0xFF1F6F9F),
        transfer = Color(0xFF3D7EA6),
        warn = Color(0xFFD98C3E),
        header = Color(0xFF0E3A5C),
        onHeader = Color(0xFFF2F8FC),
        bandEven = Color(0xFFFFFFFF),
        bandOdd = Color(0xFFE6F0F7),
        accent = Color(0xFF2F9ED8),
        isDark = false,
        categoryPalette = DefaultCategories
    )
    AppThemeOption.MIDNIGHT -> AppColorPalette(
        background = Color(0xFF12151C),
        surface = Color(0xFF1C212B),
        border = Color(0xFF2E3645),
        text = Color(0xFFE8ECF2),
        textSoft = Color(0xFF9AA3B5),
        income = Color(0xFF3DCF8E),
        expense = Color(0xFFE06A5E),
        savings = Color(0xFF5BA3E0),
        transfer = Color(0xFFA78BDB),
        warn = Color(0xFFE0B15A),
        header = Color(0xFF2A3344),
        onHeader = Color(0xFFE8ECF2),
        bandEven = Color(0xFF1C212B),
        bandOdd = Color(0xFF242B38),
        accent = Color(0xFF5BA3E0),
        isDark = true,
        categoryPalette = DefaultCategories
    )
    AppThemeOption.AMBER -> AppColorPalette(
        background = Color(0xFFFBF7F0),
        surface = Color(0xFFFFFCF7),
        border = Color(0xFFE8DCC8),
        text = Color(0xFF2A2118),
        textSoft = Color(0xFF7A6A55),
        income = Color(0xFF2F8A55),
        expense = Color(0xFFB84A3A),
        savings = Color(0xFF2F6FB0),
        transfer = Color(0xFF8A6A3D),
        warn = Color(0xFFC9892E),
        header = Color(0xFF5C3D12),
        onHeader = Color(0xFFFFF8EC),
        bandEven = Color(0xFFFFFCF7),
        bandOdd = Color(0xFFF3E8D6),
        accent = Color(0xFFC9892E),
        isDark = false,
        categoryPalette = DefaultCategories
    )
    AppThemeOption.SLATE -> AppColorPalette(
        background = Color(0xFFF4F5F7),
        surface = Color(0xFFFFFFFF),
        border = Color(0xFFD8DCE3),
        text = Color(0xFF1E2430),
        textSoft = Color(0xFF6A7383),
        income = Color(0xFF1F8A5F),
        expense = Color(0xFFC0463B),
        savings = Color(0xFF3F6F9F),
        transfer = Color(0xFF6B7280),
        warn = Color(0xFFD98C3E),
        header = Color(0xFF2A3344),
        onHeader = Color(0xFFF4F6F9),
        bandEven = Color(0xFFFFFFFF),
        bandOdd = Color(0xFFE9EDF3),
        accent = Color(0xFF4B6A8A),
        isDark = false,
        categoryPalette = DefaultCategories
    )
}

/**
 * Snapshot-backed palette so existing `FinanceColors.X` call sites recompose
 * when the user switches theme in Settings.
 */
object FinanceColors {
    private val classic = AppThemeOption.CLASSIC.palette()

    var Background by mutableStateOf(classic.background)
        private set
    var Surface by mutableStateOf(classic.surface)
        private set
    var Border by mutableStateOf(classic.border)
        private set
    var Text by mutableStateOf(classic.text)
        private set
    var TextSoft by mutableStateOf(classic.textSoft)
        private set
    var Income by mutableStateOf(classic.income)
        private set
    var Expense by mutableStateOf(classic.expense)
        private set
    var Savings by mutableStateOf(classic.savings)
        private set
    var Transfer by mutableStateOf(classic.transfer)
        private set
    var Warn by mutableStateOf(classic.warn)
        private set
    var Header by mutableStateOf(classic.header)
        private set
    var OnHeader by mutableStateOf(classic.onHeader)
        private set
    var BandEven by mutableStateOf(classic.bandEven)
        private set
    var BandOdd by mutableStateOf(classic.bandOdd)
        private set
    var Accent by mutableStateOf(classic.accent)
        private set
    var CategoryPalette by mutableStateOf(classic.categoryPalette)
        private set

    fun apply(palette: AppColorPalette) {
        Background = palette.background
        Surface = palette.surface
        Border = palette.border
        Text = palette.text
        TextSoft = palette.textSoft
        Income = palette.income
        Expense = palette.expense
        Savings = palette.savings
        Transfer = palette.transfer
        Warn = palette.warn
        Header = palette.header
        OnHeader = palette.onHeader
        BandEven = palette.bandEven
        BandOdd = palette.bandOdd
        Accent = palette.accent
        CategoryPalette = palette.categoryPalette
    }
}

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp),
    labelSmall = TextStyle(fontSize = 12.sp)
)

@Composable
fun FinanceTrackerTheme(
    theme: AppThemeOption = AppThemeOption.CLASSIC,
    content: @Composable () -> Unit
) {
    val palette = theme.palette()
    SideEffect { FinanceColors.apply(palette) }

    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.textSoft,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.bandOdd,
            outline = palette.border,
            error = palette.expense
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.textSoft,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.bandOdd,
            outline = palette.border,
            error = palette.expense
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}
