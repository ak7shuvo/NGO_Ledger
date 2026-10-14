package com.ngoloanmanager.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Brand palette (PETRA identity refined to the premium blue-indigo-teal direction) ----
// Existing constant names are kept so every current reference keeps working.
val BrandNavy = Color(0xFF10172A)      // midnight navy: primary / dark background
val BrandIndigo = Color(0xFF283B78)    // deep indigo: gradient mid-tone, dark containers
val BrandBlue = Color(0xFF4F7CFF)      // electric blue: gradient highlight, links
val BrandTeal = Color(0xFF36D6C5)      // teal: buttons, accents on dark surfaces
val BrandLavender = Color(0xFFB8A7FF)  // soft lavender: ambient glow only, never text
val BrandGold = Color(0xFFD4AF37)      // PETRA gold highlight (decorative / on dark surfaces)
val BrandLightGray = Color(0xFFF6F7FC) // warm off-white background

// ---- Accessible text-safe variants (>= 4.5:1 on white surfaces) ----
// BrandTeal and BrandGold are too light to use as text on white, so light mode uses these.
val TealOnLight = Color(0xFF0B7A71)
val GoldOnLight = Color(0xFF8A6D0B)
val SuccessGreen = Color(0xFF0D7A4F)
val SuccessGreenDark = Color(0xFF4ADE80)

// ---- Neutral / semantic helpers ----
val IndigoTint = Color(0xFFE6ECFF)
val TealTint = Color(0xFFD8F7F3)
val GoldTint = Color(0xFFF8F0D2)
val SlateText = Color(0xFF5F6A82) // secondary text; >= 4.5:1 on both white and the off-white background
val SlateOutline = Color(0xFFCBD3E4)
val SlateOutlineSoft = Color(0xFFE3E8F4)
val ErrorRed = Color(0xFFB3261E)
val ErrorContainer = Color(0xFFFDECEA)

// ---- Dark theme (midnight-navy based) ----
val DarkBg = BrandNavy
val DarkSurface = Color(0xFF1A2238)
val DarkSurfaceVariant = Color(0xFF242E4A)
val DarkTealContainer = Color(0xFF1E3A6E) // name kept for compatibility; now an indigo-blue container
