package com.github.fhilgers.compose.application.common.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.fhilgers.compose.application.common.Tooltip

@Composable
fun BlockIcon(size: Dp = 24.dp) {
//    val i18n = DI.get<I18nView>()
    Tooltip({ Text("i18n.block()") }) {
        Icon(Icons.Outlined.Lock, "i18n.block()", Modifier.size(size))
    }
}
