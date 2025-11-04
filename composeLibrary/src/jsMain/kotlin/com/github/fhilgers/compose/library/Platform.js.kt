@file:OptIn(InternalComposeUiApi::class)

package com.github.fhilgers.compose.library

import androidx.compose.ui.InternalComposeUiApi

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()