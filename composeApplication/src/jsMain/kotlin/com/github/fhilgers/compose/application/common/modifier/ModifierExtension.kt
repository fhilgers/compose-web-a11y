package com.github.fhilgers.compose.application.common.modifier

import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.github.fhilgers.compose.application.theme.components.buttonPointerModifier


infix fun Modifier.thenNullable(other: Modifier?): Modifier =
    if (other === Modifier) this else CombinedModifier(this, other ?: Modifier)

fun Modifier.blockPointerInput(): Modifier =
    this.pointerInput(Unit) {}
        .buttonPointerModifier(enabled = false)

fun Modifier.gesturesDisabled(disabled: Boolean = true) =
    if (disabled) this.pointerInput(Unit) {
        awaitPointerEventScope {
            // we should wait for all new pointer events
            while (true) {
                awaitPointerEvent(pass = PointerEventPass.Initial)
                    .changes
                    .forEach(PointerInputChange::consume)
            }
        }
    } else this
