package com.github.fhilgers.compose.application.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.fhilgers.compose.application.theme.CurrentThemeSettings
import com.github.fhilgers.compose.application.theme.ExperimentalThemingApi
import com.github.fhilgers.compose.application.theme.messengerDpConstants
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalThemingApi::class)
@Composable
fun ColumnScope.WizardImage(
    drawableResource: DrawableResource,
    contentDescription: String?,
    height: Dp,
    boxWithConstraintsScope: BoxWithConstraintsScope,
) {
    Image(
        painterResource(drawableResource),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(if (CurrentThemeSettings.isDarkMode()) Color.White else Color.Black),
        modifier = Modifier
            .padding(
                horizontal =
                    if (boxWithConstraintsScope.maxWidth < 400.dp) MaterialTheme.messengerDpConstants.verySmall
                    else MaterialTheme.messengerDpConstants.middle,
                vertical = MaterialTheme.messengerDpConstants.verySmall,
            )
            .align(Alignment.CenterHorizontally)
            .fillMaxWidth()
            .height(height)
    )
}

@OptIn(ExperimentalThemingApi::class)
@Composable
fun WizardImage(
    drawableResource: DrawableResource,
    contentDescription: String?,
    width: Dp,
    height: Dp = width,
) {
    Image(
        painterResource(drawableResource),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(if (CurrentThemeSettings.isDarkMode()) Color.White else Color.Black),
        modifier = Modifier
            .width(width)
            .height(height)
    )
}
