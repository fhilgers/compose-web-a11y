
package com.github.fhilgers.compose.library

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlin.random.Random


@get:Composable
private inline val isDarkTheme: Boolean
    get() = isSystemInDarkTheme()


@get:Composable
@get:RequiresApi(Build.VERSION_CODES.S)
private inline val dynamicColorScheme: ColorScheme
    get() = if (isDarkTheme) dynamicDarkColorScheme(LocalContext.current)
    else dynamicLightColorScheme(LocalContext.current)


@get:Composable
private inline val expressiveColorScheme: ColorScheme
    get() = if (isDarkTheme) darkColorScheme()
    else lightColorScheme()


@get:Composable
actual val colorScheme: ColorScheme
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicColorScheme
    else expressiveColorScheme


class ColorPreviewParameterProvider : PreviewParameterProvider<Color> {
    override val count: Int
        get() = 3

    override val values = sequenceOf(
        Color.Red,
        Color.Cyan,
        Color.Green,
    )
}
@PreviewScreenSizes
@PreviewFontScale
@PreviewDynamicColors
@PreviewLightDark
@Composable
fun Example(
    @PreviewParameter(ColorPreviewParameterProvider::class) color: Color
) {
    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        Surface {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var color by remember { mutableStateOf(color) }

                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawCircle(color)
                    }

                    Button(onClick = {
                        val id = Random.nextInt()
                        color = Color(id)
                    }) {
                        Text("Swap Color")
                    }
                }
            }
        }
    }
}
