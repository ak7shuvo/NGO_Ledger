package com.ngoloanmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = IndigoTint,
    onPrimaryContainer = BrandNavy,
    secondary = GoldOnLight,
    onSecondary = Color.White,
    secondaryContainer = GoldTint,
    onSecondaryContainer = BrandNavy,
    tertiary = TealOnLight,
    onTertiary = Color.White,
    tertiaryContainer = TealTint,
    onTertiaryContainer = BrandNavy,
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
    surfaceTint = BrandIndigo
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
    tertiary = BrandBlue,
    onTertiary = BrandNavy,
    tertiaryContainer = BrandIndigo,
    onTertiaryContainer = BrandLightGray,
    background = DarkBg,
    onBackground = BrandLightGray,
    surface = DarkSurface,
    onSurface = BrandLightGray,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC3CBE0),
    outline = Color(0xFF55607C),
    outlineVariant = Color(0xFF353F5C),
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

/** True when the active color scheme is a dark one (derived from the surface so it follows NgoLoanTheme). */
@Composable
@ReadOnlyComposable
fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** Accent colors that stay readable as TEXT in both themes (brand teal/gold are too light on white). */
@Composable
@ReadOnlyComposable
fun accentTeal(): Color = if (isDarkSurface()) BrandTeal else TealOnLight

@Composable
@ReadOnlyComposable
fun accentGold(): Color = if (isDarkSurface()) BrandGold else GoldOnLight

@Composable
@ReadOnlyComposable
fun accentSuccess(): Color = if (isDarkSurface()) SuccessGreenDark else SuccessGreen
