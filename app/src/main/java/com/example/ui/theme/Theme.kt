package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = PlasmaBlue,
    tertiary = WarpGreen,
    background = SpaceBlack,
    surface = DeepSpace,
    onPrimary = SpaceBlack,
    onSecondary = Color.White,
    onTertiary = SpaceBlack,
    onBackground = Color.White,
    onSurface = NebulaGrey,
    surfaceVariant = NebulaCard
)

private val LightColorScheme = lightColorScheme(
    primary = PlasmaBlue,
    secondary = CyberCyan,
    tertiary = WarpGreen,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = SpaceBlack,
    onBackground = Color(0xFF101018),
    onSurface = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for premium tech/VPN feeling by default
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the custom designed Cosmic design
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
