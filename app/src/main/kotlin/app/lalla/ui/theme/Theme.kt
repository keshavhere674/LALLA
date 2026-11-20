package app.lalla.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// LALLA Color Palette - Warm Purple/Violet
private val LallaPrimary = Color(0xFF6750A4)
private val LallaOnPrimary = Color(0xFFFFFFFF)
private val LallaPrimaryContainer = Color(0xFFEADDFF)
private val LallaOnPrimaryContainer = Color(0xFF21005D)
private val LallaSecondary = Color(0xFF958DA5)
private val LallaOnSecondary = Color(0xFFFFFFFF)
private val LallaSecondaryContainer = Color(0xFFEBDEFF)
private val LallaOnSecondaryContainer = Color(0xFF1E192B)
private val LallaTertiary = Color(0xFFB58392)
private val LallaOnTertiary = Color(0xFFFFFFFF)
private val LallaTertiaryContainer = Color(0xFFFFD8E4)
private val LallaOnTertiaryContainer = Color(0xFF31111D)
private val LallaError = Color(0xFFB3261E)
private val LallaOnError = Color(0xFFFFFFFF)
private val LallaErrorContainer = Color(0xFFF9DEDC)
private val LallaOnErrorContainer = Color(0xFF410E0B)
private val LallaBackground = Color(0xFFFFFBFE)
private val LallaOnBackground = Color(0xFF1C1B1F)
private val LallaSurface = Color(0xFFFFFBFE)
private val LallaOnSurface = Color(0xFF1C1B1F)
private val LallaSurfaceVariant = Color(0xFFE7E0EC)
private val LallaOnSurfaceVariant = Color(0xFF49454F)
private val LallaOutline = Color(0xFF79747E)

// Dark theme colors
private val LallaDarkPrimary = Color(0xFFD0BCFF)
private val LallaDarkOnPrimary = Color(0xFF381E72)
private val LallaDarkPrimaryContainer = Color(0xFF4F378B)
private val LallaDarkOnPrimaryContainer = Color(0xFFEADDFF)
private val LallaDarkSecondary = Color(0xFFCCC2DC)
private val LallaDarkOnSecondary = Color(0xFF332D41)
private val LallaDarkSecondaryContainer = Color(0xFF4A4458)
private val LallaDarkOnSecondaryContainer = Color(0xFFEBDEFF)
private val LallaDarkTertiary = Color(0xFFEFB8C8)
private val LallaDarkOnTertiary = Color(0xFF492532)
private val LallaDarkTertiaryContainer = Color(0xFF633B48)
private val LallaDarkOnTertiaryContainer = Color(0xFFFFD8E4)
private val LallaDarkError = Color(0xFFF2B8B5)
private val LallaDarkOnError = Color(0xFF601410)
private val LallaDarkErrorContainer = Color(0xFF8C1D18)
private val LallaDarkOnErrorContainer = Color(0xFFF9DEDC)
private val LallaDarkBackground = Color(0xFF1C1B1F)
private val LallaDarkOnBackground = Color(0xFFE6E1E5)
private val LallaDarkSurface = Color(0xFF1C1B1F)
private val LallaDarkOnSurface = Color(0xFFE6E1E5)
private val LallaDarkSurfaceVariant = Color(0xFF49454F)
private val LallaDarkOnSurfaceVariant = Color(0xFFCAC4D0)
private val LallaDarkOutline = Color(0xFF938F99)

val LallaLightColorScheme = lightColorScheme(
    primary = LallaPrimary,
    onPrimary = LallaOnPrimary,
    primaryContainer = LallaPrimaryContainer,
    onPrimaryContainer = LallaOnPrimaryContainer,
    secondary = LallaSecondary,
    onSecondary = LallaOnSecondary,
    secondaryContainer = LallaSecondaryContainer,
    onSecondaryContainer = LallaOnSecondaryContainer,
    tertiary = LallaTertiary,
    onTertiary = LallaOnTertiary,
    tertiaryContainer = LallaTertiaryContainer,
    onTertiaryContainer = LallaOnTertiaryContainer,
    error = LallaError,
    onError = LallaOnError,
    errorContainer = LallaErrorContainer,
    onErrorContainer = LallaOnErrorContainer,
    background = LallaBackground,
    onBackground = LallaOnBackground,
    surface = LallaSurface,
    onSurface = LallaOnSurface,
    surfaceVariant = LallaSurfaceVariant,
    onSurfaceVariant = LallaOnSurfaceVariant,
    outline = LallaOutline
)

val LallaDarkColorScheme = darkColorScheme(
    primary = LallaDarkPrimary,
    onPrimary = LallaDarkOnPrimary,
    primaryContainer = LallaDarkPrimaryContainer,
    onPrimaryContainer = LallaDarkOnPrimaryContainer,
    secondary = LallaDarkSecondary,
    onSecondary = LallaDarkOnSecondary,
    secondaryContainer = LallaDarkSecondaryContainer,
    onSecondaryContainer = LallaDarkOnSecondaryContainer,
    tertiary = LallaDarkTertiary,
    onTertiary = LallaDarkOnTertiary,
    tertiaryContainer = LallaDarkTertiaryContainer,
    onTertiaryContainer = LallaDarkOnTertiaryContainer,
    error = LallaDarkError,
    onError = LallaDarkOnError,
    errorContainer = LallaDarkErrorContainer,
    onErrorContainer = LallaDarkOnErrorContainer,
    background = LallaDarkBackground,
    onBackground = LallaDarkOnBackground,
    surface = LallaDarkSurface,
    onSurface = LallaDarkOnSurface,
    surfaceVariant = LallaDarkSurfaceVariant,
    onSurfaceVariant = LallaDarkOnSurfaceVariant,
    outline = LallaDarkOutline
)

@Composable
fun LallaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LallaDarkColorScheme else LallaLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LallaTypography,
        content = content
    )
}
