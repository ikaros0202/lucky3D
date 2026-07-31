package com.lucky3d.app.core.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Lucky3dLightColors = lightColorScheme(
    primary = Color(0xFFC50F34),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFCE8ED),
    onPrimaryContainer = Color(0xFF6B0920),
    secondary = Color(0xFF80515D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5DDE3),
    onSecondaryContainer = Color(0xFF4E2530),
    tertiary = Color(0xFF315E91),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7E3F8),
    onTertiaryContainer = Color(0xFF173F6E),
    background = Color(0xFFF8F2F0),
    onBackground = Color(0xFF2B2023),
    surface = Color(0xFFFFFBF8),
    onSurface = Color(0xFF2B2023),
    surfaceVariant = Color(0xFFF6EAE8),
    onSurfaceVariant = Color(0xFF746368),
    outline = Color(0xFFA8868D),
    outlineVariant = Color(0xFFE4CFD3),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

val Lucky3dDarkColors = darkColorScheme(
    primary = Color(0xFFFF7895),
    onPrimary = Color(0xFF4A0013),
    primaryContainer = Color(0xFF681126),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFE5B9C4),
    onSecondary = Color(0xFF442731),
    secondaryContainer = Color(0xFF5D3D47),
    onSecondaryContainer = Color(0xFFFFD9E1),
    tertiary = Color(0xFFA9C7F4),
    onTertiary = Color(0xFF0D315C),
    tertiaryContainer = Color(0xFF294A76),
    onTertiaryContainer = Color(0xFFD7E3F8),
    background = Color(0xFF171013),
    onBackground = Color(0xFFF7EDF0),
    surface = Color(0xFF21171B),
    onSurface = Color(0xFFF7EDF0),
    surfaceVariant = Color(0xFF322229),
    onSurfaceVariant = Color(0xFFCEBBC1),
    outline = Color(0xFFA67B86),
    outlineVariant = Color(0xFF5A3D45),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Immutable
data class Lucky3dSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

val Lucky3dLightSemanticColors = Lucky3dSemanticColors(
    success = Color(0xFF176B49),
    onSuccess = Color.White,
    successContainer = Color(0xFFC0EDD7),
    onSuccessContainer = Color(0xFF002115),
    warning = Color(0xFF8A5A00),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFDEA5),
    onWarningContainer = Color(0xFF2B1700),
)

val Lucky3dDarkSemanticColors = Lucky3dSemanticColors(
    success = Color(0xFF8ED7AE),
    onSuccess = Color(0xFF003822),
    successContainer = Color(0xFF0C5132),
    onSuccessContainer = Color(0xFFAAF4C9),
    warning = Color(0xFFF3C46D),
    onWarning = Color(0xFF482900),
    warningContainer = Color(0xFF684000),
    onWarningContainer = Color(0xFFFFDEA5),
)

val LocalLucky3dSemanticColors = staticCompositionLocalOf { Lucky3dLightSemanticColors }

@Immutable
data class Lucky3dVisualColors(
    val primaryDeep: Color,
    val crystalBackground: Color,
    val crystalRose: Color,
    val crystalLavender: Color,
    val crystalBorder: Color,
    val crystalHighlight: Color,
    val crystalShadow: Color,
    val latestRow: Color,
    val futureRow: Color,
)

val Lucky3dLightVisualColors = Lucky3dVisualColors(
    primaryDeep = Color(0xFF8F102B),
    crystalBackground = Color(0xFFF8F6FA),
    crystalRose = Color(0xFFFFF0F4),
    crystalLavender = Color(0xFFF0EEF8),
    crystalBorder = Color(0xFFE8DCE5),
    crystalHighlight = Color(0xFFFFFFFF),
    crystalShadow = Color(0x183B1630),
    latestRow = Color(0xFFFFF1F4),
    futureRow = Color(0xFFF4ECEE),
)

val Lucky3dDarkVisualColors = Lucky3dVisualColors(
    primaryDeep = Color(0xFF9D2946),
    crystalBackground = Color(0xFF191419),
    crystalRose = Color(0xFF382029),
    crystalLavender = Color(0xFF29243A),
    crystalBorder = Color(0xFF5A3D45),
    crystalHighlight = Color(0x33FFFFFF),
    crystalShadow = Color(0x66000000),
    latestRow = Color(0xFF3B1C27),
    futureRow = Color(0xFF281C21),
)

val LocalLucky3dVisualColors = staticCompositionLocalOf { Lucky3dLightVisualColors }
