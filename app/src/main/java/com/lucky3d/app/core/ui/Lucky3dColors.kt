package com.lucky3d.app.core.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Lucky3dLightColors = lightColorScheme(
    primary = Color(0xFF1E4E79),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E8FA),
    onPrimaryContainer = Color(0xFF092F4F),
    secondary = Color(0xFF526270),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7EF),
    onSecondaryContainer = Color(0xFF101D27),
    tertiary = Color(0xFF356E68),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF15202B),
    surface = Color(0xFFFBFCFE),
    onSurface = Color(0xFF15202B),
    surfaceVariant = Color(0xFFE4E8ED),
    onSurfaceVariant = Color(0xFF41484F),
    outline = Color(0xFF727A83),
    outlineVariant = Color(0xFFC2C7CE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val Lucky3dDarkColors = darkColorScheme(
    primary = Color(0xFF9CCBFA),
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF164A72),
    onPrimaryContainer = Color(0xFFD0E7FF),
    secondary = Color(0xFFBAC8D5),
    onSecondary = Color(0xFF25323C),
    secondaryContainer = Color(0xFF344955),
    onSecondaryContainer = Color(0xFFDCE7EF),
    tertiary = Color(0xFF9FD4CD),
    onTertiary = Color(0xFF003733),
    background = Color(0xFF0E1720),
    onBackground = Color(0xFFDDE4EA),
    surface = Color(0xFF121C27),
    onSurface = Color(0xFFDDE4EA),
    surfaceVariant = Color(0xFF404850),
    onSurfaceVariant = Color(0xFFC0C7CE),
    outline = Color(0xFF8A929B),
    outlineVariant = Color(0xFF40484F),
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
    success = Color(0xFF276B48),
    onSuccess = Color.White,
    successContainer = Color(0xFFB9F0CF),
    onSuccessContainer = Color(0xFF002112),
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
