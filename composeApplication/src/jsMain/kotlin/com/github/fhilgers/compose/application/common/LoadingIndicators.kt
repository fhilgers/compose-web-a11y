package com.github.fhilgers.compose.application.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.fhilgers.compose.application.theme.components
import com.github.fhilgers.compose.application.theme.components.ThemedProgressIndicator

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(Modifier.fillMaxWidth().then(modifier)) {
        ThemedProgressIndicator(
            Modifier.align(Alignment.Center),
            MaterialTheme.components.circularProgressIndicator
        )
    }
}
