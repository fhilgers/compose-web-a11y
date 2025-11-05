package com.github.fhilgers.compose.application.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.github.fhilgers.compose.application.theme.messengerDpConstants

@Composable
fun Paragraphs(
    modifier: Modifier = Modifier,
    spaceBetween: Dp = MaterialTheme.messengerDpConstants.small,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(spaceBetween), modifier = modifier) {
        content()
    }
}
