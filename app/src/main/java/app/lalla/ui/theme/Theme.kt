package app.lalla.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── LALLA Premium Dark Palette ──────────────────────────────────────────────
// Near-black backgrounds with vivid violet + cyan accent
val DarkBackground      = Color(0xFF0D0D16)
val DarkSurface         = Color(0xFF161625)
val DarkSurfaceVariant  = Color(0xFF1E1E32)
val DarkSurfaceContainer= Color(0xFF1A1A2C)

val VividViolet         = Color(0xFF7C4DFF)   // Primary
val VioletContainer     = Color(0xFF3D2080)
val LightViolet         = Color(0xFFD0BCFF)   // onPrimary in dark

val CyanAccent          = Color(0xFF00E5FF)   // Tertiary / secondary accent
val CyanContainer       = Color(0xFF004D57)

val UserBubble          = Color(0xFF4A148C)   // Deep purple for user chat
val AiBubble            = Color(0xFF1E1E32)   // Dark navy for AI chat

val ErrorRed            = Color(0xFFCF6679)
val ErrorContainer      = Color(0xFF8C1D18)

val OnDarkPrimary       = Color(0xFF1A0060)
val OnDarkSurface       = Color(0xFFECE6F0)
val OnDarkSurfaceVar    = Color(0xFFCAC4D0)
val DarkOutline         = Color(0xFF4A4760)

val PremiumDarkColorScheme = darkColorScheme(
    primary              = VividViolet,
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = VioletContainer,
    onPrimaryContainer   = LightViolet,
    secondary            = CyanAccent,
    onSecondary          = Color(0xFF003640),
    secondaryContainer   = CyanContainer,
    onSecondaryContainer = Color(0xFF97EFFF),
    tertiary             = Color(0xFFD580FF),
    onTertiary           = Color(0xFF3C0066),
    tertiaryContainer    = Color(0xFF5A0099),
    onTertiaryContainer  = Color(0xFFF1BCFF),
    error                = ErrorRed,
    onError              = Color(0xFF601410),
    errorContainer       = ErrorContainer,
    onErrorContainer     = Color(0xFFF9DEDC),
    background           = DarkBackground,
    onBackground         = OnDarkSurface,
    surface              = DarkSurface,
    onSurface            = OnDarkSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = OnDarkSurfaceVar,
    outline              = DarkOutline,
    outlineVariant       = Color(0xFF2E2B40),
    scrim                = Color(0xFF000000),
    inverseSurface       = Color(0xFFECE6F0),
    inverseOnSurface     = Color(0xFF1C1B1F),
    inversePrimary       = VioletContainer,
    surfaceTint          = VividViolet,
)

@Composable
fun LallaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Always use premium dark scheme
    val colorScheme = PremiumDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = DarkBackground.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LallaTypography,
        content = content
    )
}
