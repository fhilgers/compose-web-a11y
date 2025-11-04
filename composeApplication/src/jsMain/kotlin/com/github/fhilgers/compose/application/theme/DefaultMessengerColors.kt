package com.github.fhilgers.compose.application.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@OptIn(ExperimentalThemingApi::class)
internal val DefaultMessengerColors: MessengerColors
    @Composable
    get() {
        val settings = CurrentThemeSettings
//        val accentColor = settings.accentColor ?: DefaultMessengerColors.
//
//        return if (settings.isDarkMode()) {
//            DI.get<ThemeDarkMessengerColors>().create(accentColor)
//        } else {
//            DI.get<ThemeLightMessengerColors>().create(accentColor)
//        }
        // TODO(-)
        return MaterialTheme.messengerColors
    }
