package com.lucky3d.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky3d.app.core.ui.LocalLucky3dSemanticColors
import com.lucky3d.app.core.ui.LocalLucky3dVisualColors
import com.lucky3d.app.core.ui.Lucky3dDarkColors
import com.lucky3d.app.core.ui.Lucky3dDarkSemanticColors
import com.lucky3d.app.core.ui.Lucky3dDarkVisualColors
import com.lucky3d.app.core.ui.Lucky3dLightColors
import com.lucky3d.app.core.ui.Lucky3dLightSemanticColors
import com.lucky3d.app.core.ui.Lucky3dLightVisualColors

@Composable
fun Lucky3DTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val semanticColors = if (darkTheme) {
        Lucky3dDarkSemanticColors
    } else {
        Lucky3dLightSemanticColors
    }
    val visualColors = if (darkTheme) {
        Lucky3dDarkVisualColors
    } else {
        Lucky3dLightVisualColors
    }
    CompositionLocalProvider(
        LocalLucky3dSemanticColors provides semanticColors,
        LocalLucky3dVisualColors provides visualColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) Lucky3dDarkColors else Lucky3dLightColors,
            typography = Lucky3dTypography,
            shapes = Lucky3dShapes,
            content = content,
        )
    }
}

private val DefaultTypography = Typography()

private val Lucky3dTypography = Typography(
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = DefaultTypography.headlineSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = DefaultTypography.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyMedium = DefaultTypography.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = DefaultTypography.bodySmall.copy(
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
)

private val Lucky3dShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
