package com.github.fhilgers.compose.application.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
data class ThemeSettings(
    val themeMode: ThemeMode,
    val isHighContrast: Boolean,
    val accentColor: Color?,
) {
    @Composable
    fun isDarkMode(): Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }
}

//@ExperimentalThemingApi
//val CurrentThemeSettings: ThemeSettings
//    @Composable
//    get() = DI.getOrNull<MatrixMessengerSettingsHolder>()
//        ?.map {
//            ThemeSettings(
//                it.base.themeMode,
//                it.base.isHighContrast,
//                it.base.accentColor?.let { Color(it.toULong()) })
//        }
//        ?.distinctUntilChanged()
//        ?.collectAsState(null)?.value
//        ?: ThemeSettings(ThemeMode.DEFAULT, false, null)


@ExperimentalThemingApi
val CurrentThemeSettings: ThemeSettings = ThemeSettings(
    themeMode = ThemeMode.DARK,
    isHighContrast = false,
    accentColor = null,
)

@Serializable
enum class ThemeMode {
    @SerialName("default")
    DEFAULT,

    @SerialName("light")
    LIGHT,

    @SerialName("dark")
    DARK
}
