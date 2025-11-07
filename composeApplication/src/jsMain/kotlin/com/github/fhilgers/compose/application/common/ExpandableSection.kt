package com.github.fhilgers.compose.application.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.fhilgers.compose.application.common.modifier.focusHighlighting
import com.github.fhilgers.compose.application.theme.components.buttonPointerModifier

@Composable
fun ExpandableSection(
    heading: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ExpandableSection(
        heading = { Text(heading, style = MaterialTheme.typography.titleMedium) },
        modifier = modifier,
        icon = icon,
        content = content,
    )
}

@Stable
fun Modifier.animateRotation(rotation: State<Float>) = drawWithContent {
    with(drawContext) {
        canvas.save()
        try {
            transform.rotate(rotation.value)
            drawContent()
        } finally {
            canvas.restore()
        }
    }
}

@Composable
fun ExpandableSection(
    heading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val rotateState = animateFloatAsState(if (expanded.value) 180F else 0F)

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .focusHighlighting(interactionSource, shape = MaterialTheme.shapes.small)
            .fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clickable(interactionSource, LocalIndication.current) {
                        expanded.value = !expanded.value
                    }.semantics {
                        role = Role.Button
                        toggleableState = if (expanded.value) ToggleableState.On else ToggleableState.Off
                    }
                    .buttonPointerModifier(true).padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                }
                heading()
                Spacer(Modifier.weight(1F).padding(end = 10.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.animateRotation(rotateState)
                )
            }
            AnimatedVisibility(expanded.value) {
                Column(Modifier.padding(8.dp)) {
                    content()
                }
            }
        }
    }
}
