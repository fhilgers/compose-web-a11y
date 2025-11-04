package com.github.fhilgers.compose.application.theme.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.fhilgers.compose.application.theme.components

@Composable
fun ThemedLabel(
    text: String,
    style: SurfaceStyle = MaterialTheme.components.label,
) {
    ThemedSurface(style = style) {
        Text(text = text, maxLines = 1)
    }
}
