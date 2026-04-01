package com.fiserv.payments.merchantdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = FiservOrange,
    onPrimary = White,
    surface = White,
    onSurface = DarkText,
    surfaceVariant = FiservGray,
    onSurfaceVariant = SubtleText,
    background = White,
    onBackground = DarkText,
    error = ErrorRed,
    outline = DividerColor
)

@Composable
fun MerchantDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MerchantTypography,
        content = content
    )
}
