package com.cleanvault.wallet.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CleanVaultColorScheme = darkColorScheme(
    primary = VaultGreen,
    onPrimary = VaultBlack,
    primaryContainer = VaultGreenSubtle,
    onPrimaryContainer = VaultGreen,
    surface = VaultDarkSurface,
    onSurface = VaultTextPrimary,
    surfaceVariant = VaultSurfaceHighlight,
    onSurfaceVariant = VaultTextSecondary,
    background = VaultBlack,
    onBackground = VaultTextPrimary,
    outline = VaultCardBorder,
    error = VaultRed
)

@Composable
fun CleanVaultTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = CleanVaultColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VaultBlack.toArgb()
                window.navigationBarColor = VaultBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
