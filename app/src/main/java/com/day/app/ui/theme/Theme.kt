package com.day.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =====================================================================
// THE DAY — Dark Luxury Minimal Color Scheme
// =====================================================================

private val DarkMonochromeColorScheme = darkColorScheme(
    primary = DayTextPrimary,
    onPrimary = DayBackground,
    primaryContainer = DaySurfaceHighlight,
    onPrimaryContainer = DayTextPrimary,
    secondary = DayTextSecondary,
    onSecondary = DayBackground,
    secondaryContainer = DaySurfaceElevated,
    onSecondaryContainer = DayTextPrimary,
    tertiary = DayTextTertiary,
    onTertiary = DayTextPrimary,
    background = DayBackground,
    onBackground = DayTextPrimary,
    surface = DaySurface,
    onSurface = DayTextPrimary,
    surfaceVariant = DaySurfaceElevated,
    onSurfaceVariant = DayTextSecondary,
    outline = DayBorder,
    outlineVariant = DayBorderSubtle,
    error = DayDestructive,
    onError = DayTextPrimary
)

@Composable
fun DAYTheme(
    darkTheme: Boolean = true, // THE DAY is a dedicated dark monochrome system
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkMonochromeColorScheme,
        typography = DayTypography,
        content = content
    )
}
