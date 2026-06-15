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

private val LightColorScheme =
  lightColorScheme(
    primary = SolidPrimary,
    secondary = DarkAccent,
    background = SpaceBackground,
    surface = SpaceCard,
    onPrimary = Color.White,
    onSecondary = PrimaryText,
    onBackground = PrimaryText,
    onSurface = PrimaryText,
    outline = LightBorder,
    surfaceVariant = SearchBackground
  )

private val DarkColorScheme = LightColorScheme // Force Clean Minimalism light mode unconditionally

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
