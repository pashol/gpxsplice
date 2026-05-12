package com.example.gpxsplice.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()
private val AppShapes = Shapes()
private val AppTypography = Typography()

fun shouldUseDynamicColor(dynamicColorEnabled: Boolean, sdkInt: Int): Boolean {
    return dynamicColorEnabled && sdkInt >= Build.VERSION_CODES.S
}

@Composable
fun gpxSpliceColorScheme(
    darkTheme: Boolean,
    dynamicColorEnabled: Boolean,
): ColorScheme {
    val context = LocalContext.current
    return when {
        shouldUseDynamicColor(dynamicColorEnabled, Build.VERSION.SDK_INT) && darkTheme -> dynamicDarkColorScheme(context)
        shouldUseDynamicColor(dynamicColorEnabled, Build.VERSION.SDK_INT) -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
}

@Composable
fun GpxSpliceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColorEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = gpxSpliceColorScheme(darkTheme = darkTheme, dynamicColorEnabled = dynamicColorEnabled),
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
