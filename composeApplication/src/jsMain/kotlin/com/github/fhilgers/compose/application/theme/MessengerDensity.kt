package com.github.fhilgers.compose.application.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val SystemDensity = compositionLocalOf<Density> { error("compositionLocal not defined") }

val DefaultMessengerDensity: Density
    @Composable
    get() = CurrentSizeSettings.toDensity(LocalDensity.current, DefaultSizesImpl())
//get() = CurrentSizeSettings.toDensity(LocalDensity.current, DI.get<DefaultSizes>())
