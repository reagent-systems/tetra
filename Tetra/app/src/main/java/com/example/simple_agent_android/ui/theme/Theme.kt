package com.example.simple_agent_android.ui.theme

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode

private val DarkColorScheme = darkColorScheme(
    primary = ReagentGreen,
    secondary = ReagentBlue,
    tertiary = ReagentGray,
    background = ReagentBlack,
    surface = ReagentDark,
    onPrimary = ReagentBlack,
    onSecondary = ReagentWhite,
    onTertiary = ReagentWhite,
    onBackground = ReagentWhite,
    onSurface = ReagentWhite
)

private val LightColorScheme = lightColorScheme(
    primary = ReagentGreen,
    secondary = ReagentBlue,
    tertiary = ReagentGray,
    background = ReagentBlack,
    surface = ReagentDark,
    onPrimary = ReagentBlack,
    onSecondary = ReagentWhite,
    onTertiary = ReagentWhite,
    onBackground = ReagentWhite,
    onSurface = ReagentWhite

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun SimpleAgentAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}