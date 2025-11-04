package com.github.fhilgers.compose.application.theme

import androidx.compose.ui.graphics.Color

interface DefaultAccentColor {
    val value: Color
}

class DefaultAccentColorImpl : DefaultAccentColor {
    override val value: Color = Color(0xfffa7c2e)
}
