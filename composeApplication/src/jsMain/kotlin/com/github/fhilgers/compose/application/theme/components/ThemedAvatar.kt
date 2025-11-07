package com.github.fhilgers.compose.application.theme.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.fhilgers.compose.application.common.Tooltip
import com.github.fhilgers.compose.application.theme.SystemDensity
import com.github.fhilgers.compose.application.theme.components
import com.github.fhilgers.compose.application.theme.messengerColors
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

data class AvatarStyle(
    val color: Color,
    val contentColor: Color,
    val outerBorder: BorderStroke,
    val innerBorder: BorderStroke,
    val shape: Shape,
    val badgeSize: Dp,
    val badgeShape: Shape,
) {
    companion object {
        @Composable
        fun default(
            color: Color = MaterialTheme.colorScheme.primaryContainer,
            contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
            outerBorder: BorderStroke = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            innerBorder: BorderStroke = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            shape: Shape = CircleShape,
            badgeSize: Dp = 12.dp,
            badgeShape: Shape = CircleShape,
        ) = AvatarStyle(
            color = color,
            contentColor = contentColor,
            outerBorder = outerBorder,
            innerBorder = innerBorder,
            shape = shape,
            badgeSize = badgeSize,
            badgeShape = badgeShape,
        )
    }
}

enum class Presence(val value: String) {
    @SerialName("online")
    ONLINE("online"),

    @SerialName("offline")
    OFFLINE("offline"),

    @SerialName("unavailable")
    UNAVAILABLE("unavailable")
}

fun avatarSize() = 36

@Composable
fun ThemedUserAvatar(
    initials: String,
    image: ByteArray? = null,
    presence: Presence? = null,
    size: Dp = avatarSize().dp,
    style: AvatarStyle = MaterialTheme.components.avatar,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
//    val i18n = DI.get<I18nView>()
    val bitmap = remember(image) { image?.toImageBitmap() }

    val tooltip = presenceText(presence)
    tooltip?.let {
        Tooltip({ Text(tooltip) }) {
            ThemedAvatar(size, modifier, style, overlay) {
                if (bitmap != null) {
                    AvatarContentImage(bitmap, size)
                } else {
                    AvatarContentText(initials, size)
                }
            }
        }
    } ?: ThemedAvatar(size, modifier, style, overlay) {
        if (bitmap != null) {
            AvatarContentImage(bitmap, size)
        } else {
            AvatarContentText(initials, size)
        }
    }
}

@Composable
fun ThemedAvatar(
    size: Dp,
    modifier: Modifier = Modifier,
    style: AvatarStyle = MaterialTheme.components.avatar,
    overlay: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = modifier
                .size(size)
                .background(style.color, shape = style.shape)
                .border(style.outerBorder, style.shape)
                .border(style.innerBorder.let { it.copy(width = it.width + style.outerBorder.width) }, style.shape)
                .clip(style.shape),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides style.contentColor,
            ) {
                content()
            }
        }
        overlay()
    }
}

@Composable
fun AvatarContentImage(image: ImageBitmap, size: Dp) {
    Image(
        image,
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit,
        contentDescription = null
    )
}

@Composable
fun AvatarContentIcon(icon: ImageVector, size: Dp) {
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(size * 0.6f)
    )
}

@Composable
fun AvatarContentText(text: String, size: Dp) {
    Text(
        text,
        modifier = Modifier.semantics { this.text = AnnotatedString("") },
        textAlign = TextAlign.Center,
        fontSize = with(SystemDensity.current) { size.toSp() * 0.4f },
    )
}

@Composable
fun AvatarPresenceBadge(
    presence: Presence?,
    style: AvatarStyle = MaterialTheme.components.avatar,
) {
    if (presence == null) return

    val shape = when (presence) {
        Presence.UNAVAILABLE -> MoonShape()
        else -> style.badgeShape
    }

    val icon = when (presence) {
        Presence.OFFLINE -> Icons.Outlined.Close
        else -> null
    }

    val color = when (presence) {
        Presence.ONLINE -> MaterialTheme.messengerColors.presenceOnline
        Presence.OFFLINE -> MaterialTheme.messengerColors.presenceOffline
        Presence.UNAVAILABLE -> MaterialTheme.messengerColors.presenceUnavailable
    }

    Box(
        Modifier.size(style.badgeSize)
            .background(color, shape)
            .border(style.innerBorder, shape)
    ) {
        if (icon != null) {
            val brush = style.innerBorder.brush
            val color = if (brush is SolidColor) brush.value else MaterialTheme.colorScheme.surface
            Icon(
                icon,
                contentDescription = presenceText(presence),
                modifier = Modifier.align(Alignment.Center).size(style.badgeSize * 0.75f),
                tint = color,
            )
        }
    }
}

@Composable
private fun presenceText(
    presence: Presence?,
): String? {
    return when (presence) {
        Presence.ONLINE -> "i18n.presenceOnline()"
        Presence.OFFLINE -> "i18n.presenceOffline()"
        Presence.UNAVAILABLE -> "i18n.presenceUnavailable()"
        null -> null
    }
}

@OptIn(ExperimentalResourceApi::class)
fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        decodeToImageBitmap()
    } catch (e: Exception) {
//        log.error(e) { "Cannot decode image" }
        null
    }

}

class MoonShape() : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val center = size.center
        val radius = size.minDimension / 2f
        val mainCircle = Path().apply {
            addOval(Rect(center, radius))
        }
        val initialOffset = center - Offset(-radius * 0.55f, radius * 0.55f)
        val subtractCircle = Path().apply {
            addOval(Rect(initialOffset, radius * 0.55f))
        }
        val moonToSunPath = Path().apply {
            op(mainCircle, subtractCircle, PathOperation.Difference)
        }
        return Outline.Generic(moonToSunPath)
    }
}
