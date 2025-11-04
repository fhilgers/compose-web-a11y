package com.github.fhilgers.compose.library

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@get:Composable
private inline val isDarkTheme: Boolean
    get() = isSystemInDarkTheme()


@get:Composable
actual val colorScheme: ColorScheme
    get() = if (isDarkTheme) darkColorScheme()
    else lightColorScheme()