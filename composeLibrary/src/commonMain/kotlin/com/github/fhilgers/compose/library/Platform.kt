package com.github.fhilgers.compose.library

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform