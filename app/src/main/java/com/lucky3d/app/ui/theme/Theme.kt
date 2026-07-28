package com.lucky3d.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lucky3d.app.core.ui.LocalLucky3dSemanticColors
import com.lucky3d.app.core.ui.Lucky3dDarkColors
import com.lucky3d.app.core.ui.Lucky3dDarkSemanticColors
import com.lucky3d.app.core.ui.Lucky3dLightColors
import com.lucky3d.app.core.ui.Lucky3dLightSemanticColors

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
    CompositionLocalProvider(LocalLucky3dSemanticColors provides semanticColors) {
    MaterialTheme(
            colorScheme = if (darkTheme) Lucky3dDarkColors else Lucky3dLightColors,
            typography = Lucky3dTypography,
            shapes = Lucky3dShapes,
        content = content,
    )
    }
}

private val Lucky3dTypography = Typography(
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = Typography().titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyMedium = Typography().bodyMedium.copy(lineHeight = 22.sp),
    bodySmall = Typography().bodySmall.copy(lineHeight = 19.sp),
)

private val Lucky3dShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)
