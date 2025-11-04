package com.github.fhilgers.compose.application.theme.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.fhilgers.compose.application.theme.SystemDensity
import com.github.fhilgers.compose.application.theme.components
import com.github.fhilgers.compose.application.theme.messengerColors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.SerialName
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.time.Duration

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
val EscapeKeyPressed = compositionLocalOf<Flow<Unit>> { flowOf() }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    longPressDelay: Duration = MaterialTheme.components.tooltip.longPressDelay,
    hoverShowDelay: Duration = MaterialTheme.components.tooltip.hoverShowDelay,
    hoverHideDelay: Duration = MaterialTheme.components.tooltip.hoverHideDelay,
    content: @Composable () -> Unit,
) {

    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    // We need to hoist those tasks here to allow keeping the tooltip open when hovering over the tooltip itself
    // (which is not part of the TooltipArea).
    var showTask: Job? = null
    var hideTask: Job? = null

    val showTooltip = {
        hideTask?.cancel()
        hideTask = null

        if (showTask == null) {
            showTask = scope.launch {
                delay(hoverShowDelay)
                tooltipState.show(MutatePriority.PreventUserInput)
                showTask = null
            }
        }
    }

    val hideTooltip = {
        showTask?.cancel()
        showTask = null

        if (hideTask == null && tooltipState.isPersistent.not()) {
            hideTask = scope.launch {
                delay(hoverHideDelay)
                tooltipState.dismiss()
                hideTask = null
            }
        }
    }

    val escapeKeyPressed = EscapeKeyPressed.current
    LaunchedEffect(Unit) {
        escapeKeyPressed.collect {
            hideTooltip()
        }
    }

    TooltipBox(
        modifier = modifier
            .tooltipGestures(
                enabled = enabled,
                state = tooltipState,
                longPressDelay = longPressDelay,
                showTooltip = showTooltip,
                hideTooltip = hideTooltip,
            )
            .tooltipAnchorSemantics("i18n.commonShowTooltip()", enabled, tooltipState, scope),
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            TooltipSurface(
                showTooltip = showTooltip,
                hideTooltip = hideTooltip,
            ) { tooltip() }
        },
        state = tooltipState,
        enableUserInput = false,
    ) {
        Box(
            Modifier
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        if (enabled) {
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                delay(hoverShowDelay)
                                tooltipState.show()
                            }
                        }
                    } else {
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            delay(hoverHideDelay)
                            tooltipState.dismiss()
                        }
                    }
                }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipSurface(
    style: TooltipStyle = MaterialTheme.components.tooltip,
    showTooltip: () -> Unit,
    hideTooltip: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.pointerInput(Unit) {
            coroutineScope {
                awaitPointerEventScope {
                    val pass = PointerEventPass.Main
                    while (true) {
                        val event = awaitPointerEvent(pass)
                        val inputType = event.changes[0].type
                        if (inputType == PointerType.Mouse) {
                            when (event.type) {
                                PointerEventType.Enter -> {
                                    showTooltip()
                                }

                                PointerEventType.Exit -> {
                                    hideTooltip()
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = style.shape,
        color = style.colors.containerColor,
        tonalElevation = style.tonalElevation,
        shadowElevation = style.shadowElevation
    ) {
        Box(
            modifier = Modifier
                .sizeIn(
                    minWidth = 40.dp,
                    maxWidth = 600.dp,
                    minHeight = 24.dp
                )
                .padding(8.dp, 4.dp)
                .padding(style.contentPadding)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides style.colors.contentColor,
                LocalTextStyle provides style.textStyle,
                content = content
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.tooltipGestures(
    enabled: Boolean,
    state: TooltipState,
    longPressDelay: Duration,
    showTooltip: () -> Unit,
    hideTooltip: () -> Unit,
): Modifier =
    if (enabled) {
        pointerInput(state) {
            coroutineScope {
                awaitEachGesture {
                    // Long press will finish before or after show so keep track of it, in a
                    // flow to handle both cases
                    val isLongPressedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
                    val longPressTimeout = longPressDelay
                    val pass = PointerEventPass.Initial

                    // wait for the first down press
                    val inputType = awaitFirstDown(pass = pass).type

                    if (inputType == PointerType.Touch || inputType == PointerType.Stylus) {
                        try {
                            // listen to if there is up gesture
                            // within the longPressTimeout limit
                            withTimeout(longPressTimeout.inWholeMilliseconds) {
                                waitForUpOrCancellation(pass = pass)
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            // handle long press - Show the tooltip
                            launch(start = CoroutineStart.UNDISPATCHED) {
                                try {
                                    isLongPressedFlow.tryEmit(true)
                                    state.show(MutatePriority.PreventUserInput)
                                } finally {
                                    isLongPressedFlow.collectLatest { isLongPressed ->
                                        if (!isLongPressed) {
                                            state.dismiss()
                                        }
                                    }
                                }
                            }

                            // consume the children's click handling
                            // Long press may still be in progress
                            val upEvent = waitForUpOrCancellation(pass = pass)
                            upEvent?.consume()
                        } finally {
                            isLongPressedFlow.tryEmit(false)
                        }
                    }
                }
            }
        }
            .pointerInput(state) {
                coroutineScope {
                    awaitPointerEventScope {
                        val pass = PointerEventPass.Main

                        while (true) {
                            val event = awaitPointerEvent(pass)
                            val inputType = event.changes[0].type
                            if (inputType == PointerType.Mouse) {
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        showTooltip()
                                    }

                                    PointerEventType.Exit -> {
                                        hideTooltip()
                                    }
                                }
                            }
                        }
                    }
                }
            }
    } else this

@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.tooltipAnchorSemantics(
    label: String,
    enabled: Boolean,
    state: TooltipState,
    scope: CoroutineScope
): Modifier =
    if (enabled) {
        this.semantics {
            onLongClick(
                label = label,
                action = {
                    scope.launch { state.show() }
                    true
                }
            )
        }
    } else this
