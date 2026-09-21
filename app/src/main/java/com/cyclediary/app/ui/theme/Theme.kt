package com.cyclediary.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1).sp
        ),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun CycleDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
