package com.github.fhilgers.compose.application.common.modifier

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.github.fhilgers.compose.application.theme.IsFocusHighlighting
import com.github.fhilgers.compose.application.theme.messengerFocusIndicator

@Composable
fun Modifier.focusHighlighting(
    interactionSource: MutableInteractionSource,
    color: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = RectangleShape,
): Modifier = focusHighlighting(
    hasFocus = interactionSource.collectIsFocusedAsState().value,
    isFocusHighlightingActive = IsFocusHighlighting.current,
    borderWidth = MaterialTheme.messengerFocusIndicator.borderWidth,
    color = color,
    shape = shape,
)

@Stable
fun Modifier.focusHighlighting(
    hasFocus: Boolean,
    isFocusHighlightingActive: Boolean,
    borderWidth: Dp,
    color: Color,
    shape: Shape = RectangleShape,
): Modifier {
    return if (!hasFocus || !isFocusHighlightingActive) this
    else border(borderWidth, color, shape)
}
