package com.github.fhilgers.compose.library

class AndroidPlatform : Platform {
    override val name: String = "Android with Kotlin/Android"
}

actual fun getPlatform(): Platform = AndroidPlatform()