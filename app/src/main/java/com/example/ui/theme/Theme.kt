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

private val DarkColorScheme =
  darkColorScheme(
    primary = PeakBlue,
    onPrimary = DashboardNavy,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = SurfaceVariantBlue,
    secondary = PurpleGrey80,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = DownloadGreen,
    surfaceVariant = Color(0xFF1A2744),
    onSurfaceVariant = Color(0xFFB0BECF),
    surfaceContainerLowest = Color(0xFF121212),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF252528),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF353538),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DashboardNavy,
    onPrimary = DashboardCard,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = DashboardNavy,
    secondary = OnSurfaceVariantBlue,
    secondaryContainer = SecondaryContainerGray,
    onSecondaryContainer = DashboardNavy,
    tertiary = PeakBlue,
    background = DashboardBackground,
    surface = DashboardCard,
    surfaceVariant = SurfaceVariantBlue,
    onSurfaceVariant = OnSurfaceVariantBlue,
    onBackground = DashboardNavy,
    onSurface = DashboardNavy,
    surfaceContainerLowest = DashboardCard,
    surfaceContainerLow = DrawerBackground,
    surfaceContainer = SurfaceContainerMid,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFE6E6E6),
    surfaceDim = Color(0xFFD9D9D9),
    surfaceBright = DashboardCard,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
