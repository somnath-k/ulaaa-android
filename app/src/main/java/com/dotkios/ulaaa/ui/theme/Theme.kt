package com.dotkios.ulaaa.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = OceanTeal,
    onPrimary = LightSurface,
    primaryContainer = Color_OceanTealContainer,
    onPrimaryContainer = OceanTealDark,
    secondary = Coral,
    onSecondary = LightSurface,
    secondaryContainer = Color_CoralContainer,
    onSecondaryContainer = CoralDark,
    tertiary = SandAmber,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
)

private val DarkColors = darkColorScheme(
    primary = OceanTealLight,
    onPrimary = OceanTealDark,
    primaryContainer = OceanTealDark,
    onPrimaryContainer = OceanTealLight,
    secondary = CoralLight,
    onSecondary = CoralDark,
    secondaryContainer = CoralDark,
    onSecondaryContainer = CoralLight,
    tertiary = SandAmberLight,
    onTertiary = SandAmberDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
)

@Composable
fun UlaaaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Brand-first: dynamic color off by default so Ulaaa palette always shows.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = UlaaaTypography,
        shapes = UlaaaShapes,
        content = content,
    )
}
