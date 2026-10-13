package com.ngoloanmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = TealTint,
    onPrimaryContainer = BrandNavy,
    secondary = BrandGold,
    onSecondary = BrandNavy,
    secondaryContainer = GoldTint,
    onSecondaryContainer = BrandNavy,
    tertiary = BrandTeal,
    onTertiary = BrandNavy,
    background = BrandLightGray,
    onBackground = BrandNavy,
    surface = Color.White,
    onSurface = BrandNavy,
    surfaceVariant = SlateOutlineSoft,
    onSurfaceVariant = SlateText,
    outline = SlateOutline,
    outlineVariant = SlateOutlineSoft,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF410002),
    inverseSurface = BrandNavy,
    inverseOnSurface = BrandLightGray,
    inversePrimary = BrandTeal,
    surfaceTint = BrandNavy
)

private val DarkColors = darkColorScheme(
    primary = BrandTeal,
    onPrimary = BrandNavy,
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = BrandLightGray,
    secondary = BrandGold,
    onSecondary = BrandNavy,
    secondaryContainer = Color(0xFF4A3F14),
    onSecondaryContainer = GoldTint,
    tertiary = BrandTeal,
    onTertiary = BrandNavy,
    background = DarkBg,
    onBackground = BrandLightGray,
    surface = DarkSurface,
    onSurface = BrandLightGray,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = BrandLightGray,
    inverseOnSurface = BrandNavy,
    inversePrimary = BrandNavy,
    surfaceTint = BrandTeal
)

@Composable
fun NgoLoanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
