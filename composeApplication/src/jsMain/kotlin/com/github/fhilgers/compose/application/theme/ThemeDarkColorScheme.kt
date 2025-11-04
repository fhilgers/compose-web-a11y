package com.github.fhilgers.compose.application.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.absoluteValue

//private val log = KotlinLogging.logger { }

interface ThemeDarkColorScheme {
    fun create(accentColor: Color): ColorScheme
}

class ThemeDarkColorSchemeImpl : ThemeDarkColorScheme {
    override fun create(accentColor: Color): ColorScheme {
        val accentHue = accentColor.hue
        return darkColorScheme(
            primary = accentColor,
            onPrimary = md_theme_dark_onPrimary,
            primaryContainer = md_theme_dark_primaryContainer.deriveFromHue(accentHue),
            onPrimaryContainer = md_theme_dark_onPrimaryContainer,
            secondary = md_theme_dark_secondary.deriveFromHue(accentHue),
            onSecondary = md_theme_dark_onSecondary,
            secondaryContainer = md_theme_dark_secondaryContainer,
            onSecondaryContainer = md_theme_dark_onSecondaryContainer,
            tertiary = md_theme_dark_tertiary,
            onTertiary = md_theme_dark_onTertiary,
            tertiaryContainer = md_theme_dark_tertiaryContainer,
            onTertiaryContainer = md_theme_dark_onTertiaryContainer,
            error = md_theme_dark_error,
            errorContainer = md_theme_dark_errorContainer,
            onError = md_theme_dark_onError,
            onErrorContainer = md_theme_dark_onErrorContainer,
            background = md_theme_dark_background,
            onBackground = md_theme_dark_onBackground,
            surface = md_theme_dark_surface,
            onSurface = md_theme_dark_onSurface,
            surfaceVariant = md_theme_dark_surfaceVariant.deriveFromHue(accentHue),
            onSurfaceVariant = md_theme_dark_onSurfaceVariant,
            outline = md_theme_dark_outline.deriveFromHue(accentHue),
            inverseOnSurface = md_theme_dark_inverseOnSurface,
            inverseSurface = md_theme_dark_inverseSurface.deriveFromHue(accentHue),
            inversePrimary = md_theme_dark_inversePrimary.deriveFromHue(accentHue),
            surfaceTint = md_theme_dark_surfaceTint.deriveFromHue(accentHue),
            outlineVariant = md_theme_dark_outlineVariant,
            scrim = md_theme_dark_scrim,
            surfaceDim = md_theme_dark_surfaceDim.deriveFromHue(accentHue),
            surfaceBright = md_theme_dark_surfaceBright.deriveFromHue(accentHue),
            surfaceContainerLowest = md_theme_dark_surfaceContainerLowest.deriveFromHue(accentHue),
            surfaceContainerLow = md_theme_dark_surfaceContainerLow.deriveFromHue(accentHue),
            surfaceContainer = md_theme_dark_surfaceContainer.deriveFromHue(accentHue),
            surfaceContainerHigh = md_theme_dark_surfaceContainerHigh.deriveFromHue(accentHue),
            surfaceContainerHighest = md_theme_dark_surfaceContainerHighest.deriveFromHue(accentHue),
        )
//            .also { log.debug { "create default dark color scheme" } }
    }
}

fun Color.deriveFromHue(
    hue: Float,
    saturation: Float = this.saturation,
    lightness: Float = this.lightness,
    alpha: Float = this.alpha
): Color {
    val (luminance, _, _) = convert(ColorSpaces.CieLab)
    val (_, a, b) = Color.hsl(hue, saturation, lightness).convert(ColorSpaces.CieLab)
    return Color(luminance, a, b, alpha, ColorSpaces.CieLab).convert(ColorSpaces.Srgb)
}

inline val Color.hue: Float
    get() {
        val max = maxChannel
        val delta = max - minChannel
        return ((if (delta == 0F) 0F
        else when (max) {
            red -> (green - blue) / delta + (if (green < blue) 6F else 0F)
            green -> (blue - red) / delta + 2F
            blue -> (red - green) / delta + 4F
            else -> 0F
        } / 6F) * 360F).coerceIn(0F..360F)
    }
inline val Color.lightness: Float
    get() = ((maxChannel + minChannel) / 2F).coerceIn(0F..1F)

inline val Color.saturation: Float
    get() {
        val max = maxChannel
        val delta = max - minChannel
        return (if (delta == 0F) 0F
        else delta / (1F - (2F * lightness - 1F).absoluteValue)).coerceIn(0F..1F)
    }
inline val Color.minChannel: Float
    get() = minOf(red, green, blue)

inline val Color.maxChannel: Float
    get() = maxOf(red, green, blue)
