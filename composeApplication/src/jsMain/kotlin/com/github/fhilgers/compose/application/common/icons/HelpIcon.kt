package com.github.fhilgers.compose.application.common.icons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.fhilgers.compose.application.common.Tooltip

@Composable
fun HelpIcon(text: String) {
//    val i18n = DI.get<I18nView>()
    Spacer(Modifier.size(10.dp))
    Tooltip({ Text(text) }) {
        Icon(Icons.Outlined.Info, "i18n.commonHelp()", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
