package com.github.fhilgers.compose.application.common.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.fhilgers.compose.application.common.Tooltip

@Composable
fun UnencryptedIcon() {
//    val i18n = DI.get<I18nView>()
    Tooltip({ Text("i18n.roomTypeUnencrypted()") }) {
        Icon(
            Icons.Default.NoEncryption,
            "i18n.roomTypeUnencrypted()",
            Modifier.size(16.dp),
            tint = LocalContentColor.current,
        )
    }
}
