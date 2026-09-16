package com.cyberpunk.gmtool.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==========================================
// --- تعریف پالت رنگی برای حالت تاریک (تم اصلی ما) ---
// ==========================================
private val DarkColorScheme = darkColorScheme(
    primary = CyberRed,
    secondary = CyberDarkGray,
    tertiary = CyberRed,
    background = CyberBlack,
    surface = CyberCardBg,
    onPrimary = CyberBlack,
    onSecondary = CyberTextWhite,
    onTertiary = CyberBlack,
    onBackground = CyberTextWhite,
    onSurface = CyberTextWhite
)

// ==========================================
// --- تعریف پالت رنگی برای حالت روشن ---
// (چون تم بازی تاریکه، حتی تو حالت روشن هم همون رنگ‌های دارک رو اعمال می‌کنیم)
// ==========================================
private val LightColorScheme = lightColorScheme(
    primary = CyberRed,
    secondary = CyberDarkGray,
    tertiary = CyberRed,
    background = CyberBlack,
    surface = CyberCardBg,
    onPrimary = CyberBlack,
    onSecondary = CyberTextWhite,
    onTertiary = CyberBlack,
    onBackground = CyberTextWhite,
    onSurface = CyberTextWhite
)

@Composable
fun CyberpunkGMToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // داینامیک کالر رو روی false می‌ذاریم تا رنگ قرمز/مشکی ما با والپیپر گوشی کاربر عوض نشه!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // اینجا کاری کردیم که همیشه تم ما روی حالت Dark (سایبرپانکی) بمونه
        else -> DarkColorScheme
    }

    // تنظیم رنگ نوار بالای گوشی (Status Bar)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CyberBlack.toArgb() // نوار بالا مشکی میشه
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // به فایل Type.kt متصل است
        content = content
    )
}