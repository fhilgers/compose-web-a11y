package com.github.fhilgers.compose.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers

/**
 * A MultiPreview annotation for displaying a @[Composable] method using four different wallpaper
 * colors.
 *
 * Note that the app should use a dynamic theme for these previews to be different.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "Red", wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE)
@Preview(name = "Blue", wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE)
@Preview(name = "Green", wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE)
@Preview(name = "Yellow", wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE)
annotation class PreviewDynamicColors