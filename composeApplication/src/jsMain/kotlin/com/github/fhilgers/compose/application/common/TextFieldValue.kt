package com.github.fhilgers.compose.application.common

import androidx.compose.ui.text.input.TextFieldValue

fun TextFieldValue.maxLength(maxLength: Int): TextFieldValue = copy(text.take(maxLength))
