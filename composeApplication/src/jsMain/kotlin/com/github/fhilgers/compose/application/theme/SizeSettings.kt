package com.github.fhilgers.compose.application.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density

@Immutable
data class SizeSettings(
    val displaySize: Float?,
    val fontSize: Float?,
) {
    @Stable
    fun toDensity(
        system: Density,
        fallback: DefaultSizes,
    ) = Density(
        density = system.density * (displaySize ?: fallback.displaySize),
        fontScale = system.fontScale * (fontSize ?: fallback.fontSize),
    )
}

internal val CurrentSizeSettings: SizeSettings = SizeSettings(null, null)
//internal val CurrentSizeSettings: SizeSettings
//    @Composable
//    get() = DI.getOrNull<MatrixMessengerSettingsHolder>()
//        ?.map {
//            SizeSettings(it.base.displaySize, it.base.fontSize)
//        }
//        ?.distinctUntilChanged()
//        ?.collectAsState(null)?.value
//        ?: SizeSettings(null, null)
