@file:OptIn(
    InternalComposeUiApi::class, ExperimentalComposeUiApi::class,
)

package com.github.fhilgers.compose.application

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldLineLimits.Companion
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.navigation.ExperimentalBrowserHistoryApi
import com.github.fhilgers.compose.application.common.RadioSetting
import com.github.fhilgers.compose.application.common.RadioSettingOption
import com.github.fhilgers.compose.application.common.Tooltip
import com.github.fhilgers.compose.application.common.icons.BanIcon
import com.github.fhilgers.compose.application.theme.*
import com.github.fhilgers.compose.application.theme.components.*
import com.github.fhilgers.compose.library.colorScheme
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.android.annotation.KoinViewModel
import org.koin.compose.currentKoinScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.emptyParametersHolder
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.viewmodel.defaultExtras
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLProgressElement
import org.w3c.dom.ItemArrayLike
import org.w3c.dom.LOADING
import org.w3c.dom.events.EventListener

// From ComposeWindow.w3c.kt, e.g. the same file where we yeeted the semantics out of

//addTypedEvent<TouchEvent>("touchstart") { event ->
//    canvas.getBoundingClientRect().apply {
//        offset = Offset(x = left.toFloat(), y = top.toFloat())
//    }
//
//    onTouchEvent(event, offset)
//}
//
//addTypedEvent<TouchEvent>("touchmove") { event ->
//    onTouchEvent(event, offset)
//}
//
//addTypedEvent<TouchEvent>("touchend") { event ->
//    onTouchEvent(event, offset)
//}
//
//addTypedEvent<TouchEvent>("touchcancel") { event ->
//    onTouchEvent(event, offset)
//}
//
//addTypedEvent<MouseEvent>("mousedown") { event ->
//    onMouseEvent(event)
//}
//
//addTypedEvent<MouseEvent>("mouseup") { event ->
//    onMouseEvent(event)
//}
//
//addTypedEvent<MouseEvent>("mousemove") { event ->
//    onMouseEvent(event)
//}
//
//addTypedEvent<MouseEvent>("mouseenter") { event ->
//    onMouseEvent(event)
//}
//
//addTypedEvent<MouseEvent>("mouseleave") { event ->
//    onMouseEvent(event)
//}
//
//addTypedEvent<WheelEvent>("wheel") { event ->
//    onWheelEvent(event)
//}
//
//canvas.addEventListener("contextmenu", { event ->
//    event.preventDefault()
//})
//
//addTypedEvent<KeyboardEvent>("keydown") { event ->
//    processKeyboardEvent(event)
//}
//
//addTypedEvent<KeyboardEvent>("keyup") { event ->
//    processKeyboardEvent(event)
//}
//
//state.globalEvents.addDisposableEvent("focus") {
//    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
//}
//
//state.globalEvents.addDisposableEvent("blur") {
//    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//}
//
//state.globalEvents.addDisposableEvent("visibilitychange") { event ->
//    lifecycle.handleLifecycleEvent(
//        if (documentIsVisible()) Lifecycle.Event.ON_START
//        else Lifecycle.Event.ON_STOP
//    )
//}


class CanvasSemanticsOwnerListener(
    val a11yContainer: HTMLDivElement,
    val coroutineScope: CoroutineScope = MainScope(),
) : PlatformContext.SemanticsOwnerListener {

    val focusTask = Channel<() -> Unit>(capacity = 1, BufferOverflow.DROP_OLDEST)

    init {
        a11yContainer.removeAttribute("aria-live")

        coroutineScope.launch {
            focusTask.receiveAsFlow().collect {
                it()
            }
        }
    }

    override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
        if (findElement(semanticsOwner.id) != null) return

        val ownerElement = document.createElement("div") as HTMLDivElement

        ownerElement.setAttribute("owner", "")
        ownerElement.setAttribute("semantics-id", semanticsOwner.rootSemanticsNode.id.toString())

        a11yContainer.appendChild(ownerElement)
    }

    override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
        val element = checkNotNull(findElement(semanticsOwner.id)) { "owner does not exist" }

        element.remove()
    }

    val listeners = mutableMapOf<Int, EventListener>()
    val clickListeners = mutableMapOf<Int, EventListener>()

    override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
        val queue = ArrayDeque(listOf(semanticsOwner.rootSemanticsNode))

        val parent = findElement(semanticsOwner.id) ?: return
        val currentIds = collectIds(parent)

        val seen = mutableSetOf<Int>()

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            seen.add(node.id)

            when (val found = findElement(node.id)) {
                null -> {
                    // the node does not exist we need to create a new one
                    val el = basicHTMLElement(node)
                    setAttrs(el, node)

                    val parentElement = node.parent?.id?.let(::findElement) ?: a11yContainer
                    val nextElement = node.parent?.let {
                        val index = it.replacedChildren.indexOf(node).takeIf { it >= 0 } ?: return@let null
                        it.replacedChildren.getOrNull(index + 1)?.id?.let(::findElement)
                    }

                    if (nextElement != null) {
                        parentElement.insertBefore(el, nextElement)
                    } else {
                        parentElement.appendChild(el)
                    }
                }

                else -> {
                    // the node does exist, however on the first render the node typically does not have a role
                    // so on the first render we put in a div and later on need to replace it with the correct element.
                    val el = basicHTMLElement(node)
                    if (found.tagName != el.tagName) {
                        setAttrs(el, node)
                        found.replaceWith(el)
                    } else {
                        setAttrs(found, node)
                    }
                }
            }

            // TODO onLayoutChange needs to find the element again, but we already find it above so we could be more efficient
            onLayoutChange(semanticsOwner, node.id)

            queue.addAll(node.replacedChildren)
        }

        val unseen = currentIds - seen

        for (id in unseen) {
            findElement(id)?.remove()
            listeners.remove(id)
        }
    }

    override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {
        fun inner(semanticsNodeId: Int) {
            val node = findNode(semanticsNodeId, semanticsOwner.rootSemanticsNode) ?: return
            val element = findElement(semanticsNodeId) ?: return

            val rootPosition = a11yContainer.getBoundingClientRect().let {
                Offset(it.left.toFloat(), it.top.toFloat())
            }

            val density = node.layoutInfo.density.density
            val toRoot = node.layoutInfo.coordinates.localToRoot(rootPosition).div(density)
            val size = node.boundsInRoot.size.div(density)

            element.style.left = "${toRoot.x}px"
            element.style.top = "${toRoot.y}px"
            element.style.width = "${size.width}px"
            element.style.height = "${size.height}px"

            for (child in node.replacedChildren) inner(child.id)
        }

        coroutineScope.launch {
            delay(100)
            inner(semanticsOwner.id)
        }
    }

    private fun findNode(
        semanticsId: Int, parent: SemanticsNode
    ): SemanticsNode? {
        if (parent.id == semanticsId) return parent
        for (child in parent.replacedChildren) return findNode(semanticsId, child) ?: continue
        return null
    }

    private fun findElement(
        semanticsId: Int, parent: HTMLElement = a11yContainer
    ): HTMLElement? {
        if (parent.getAttribute("semantics-id")?.toInt() == semanticsId) return parent

        for (child in parent.children.asSequence().filterIsInstance<HTMLElement>()) return findElement(
            semanticsId,
            child
        ) ?: continue

        return null
    }

    private fun collectIds(
        parent: HTMLElement = a11yContainer
    ): Set<Int> {
        val id = parent.getAttribute("semantics-id")?.toInt() ?: return emptySet()
        val ids = mutableSetOf(id)

        for (child in parent.children.asSequence().filterIsInstance<HTMLElement>()) {
            ids += collectIds(child)
        }

        return ids
    }

    private fun HTMLElement.clearAll() {
        clear()

        for (attr in attributes.asSequence()
            .filter { it.name != "owner" && it.name != "semantics-id" && it.name != "style" }) removeAttributeNode(
            attr
        )
    }


    private val SemanticsOwner.id: Int
        get() = rootSemanticsNode.id

    private fun basicHTMLElement(node: SemanticsNode): HTMLElement {
        return document.createElement(
            when (node.config.getOrNull(SemanticsProperties.Role)) {
                Role.Button -> "button"
                Role.Checkbox -> "input"
                Role.Switch -> "button"
                Role.RadioButton -> "input"
                Role.Tab -> "div"
                Role.Image -> "div"
                Role.DropdownList -> when (node.config.getOrNull(SemanticsProperties.IsEditable)) {
                    true -> "input"
                    else -> "button"
                }

                Role.ValuePicker -> "div"
                Role.Carousel -> "div"
                else -> {
                    when {
                        node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) != null ->
                            "progress"

                        node.config.getOrNull(SemanticsProperties.IsEditable) != null ->
                            "input"

                        else -> "div"
                    }
                }
            }
        ) as HTMLElement
    }

    // TODO this location may be different
    private val canvas = a11yContainer.previousElementSibling?.previousElementSibling as? HTMLCanvasElement

    private fun setAttrs(el: HTMLElement, node: SemanticsNode) {
        fun <T> setIf(attr: String, prop: SemanticsPropertyKey<T>, value: (T) -> String?) =
            node.config.getOrNull(prop)?.let {
                val v = value(it) ?: return@let null
                el.setAttribute(attr, v)
            }

        fun <T> doIf(prop: SemanticsPropertyKey<T>, value: (T) -> Unit) =
            node.config.getOrNull(prop)?.let { value(it) }


        el.setAttribute("semantics-id", node.id.toString())
        el.style.position = "fixed"
        el.style.whiteSpace = "pre"

        when (node.config.getOrNull(SemanticsProperties.Role)) {
            Role.DropdownList -> {
                // https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Roles/combobox_role
                el.setAttribute("role", "combobox")
                setIf("aria-expanded", SemanticsProperties.ToggleableState) {
                    when (it) {
                        ToggleableState.On -> "true"
                        ToggleableState.Off -> "false"
                        ToggleableState.Indeterminate -> null // better nothing than something wrong
                    }
                }
            }

            Role.RadioButton -> {
                require(el is HTMLInputElement) { "Role.RadioButton is not HTMLInputElement" }
                el.setAttribute("type", "radio")
                setIf("aria-label", SemanticsProperties.Text) { it.joinToString() }
                node.config.getOrNull(SemanticsProperties.Selected)?.let {
                    el.asDynamic().checked = it
                }
            }

            Role.Checkbox -> {
                require(el is HTMLInputElement) { "Role.Checkbox is not HTMLInputElement" }
                el.setAttribute("type", "checkbox")
                setIf("aria-label", SemanticsProperties.Text) { it.joinToString() }
                node.config.getOrNull(SemanticsProperties.Selected)?.let {
                    el.asDynamic().checked = it
                }
            }

            Role.Button -> {
                setIf("aria-expanded", SemanticsProperties.ToggleableState) {
                    when (it) {
                        ToggleableState.On -> "true"
                        ToggleableState.Off -> "false"
                        ToggleableState.Indeterminate -> null // better nothing than something wrong
                    }
                }
            }

            Role.Switch -> {
                // https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Roles/switch_role
                require(el is HTMLButtonElement) { "Role.Switch is not HTMLButtonElement" }
                el.setAttribute("role", "switch")
                setIf("aria-label", SemanticsProperties.Text) { it.joinToString() }
                setIf("aria-checked", SemanticsProperties.Selected) { it.toString() }
            }

            else -> {
                setIf("aria-label", SemanticsProperties.Text, { it.joinToString() })
            }
        }

        node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.let {
            require(el is HTMLProgressElement, { "node with ProgressBarRangeInfo is not HTMLProgressElement" })
            el.setAttribute("value", it.current.toString())
            el.setAttribute("max", it.range.endInclusive.toString())
        }

        fun areAllChildrenRadioButtons(node: SemanticsNode): Boolean {
            val innerStack = ArrayDeque(listOf(node))

            var hasRadioChild = false

            while (innerStack.isNotEmpty()) {
                val current = innerStack.removeFirst()

                val role = current.config.getOrNull(SemanticsProperties.Role)

                if (role != null && role != Role.RadioButton) return false
                if (role == Role.RadioButton) hasRadioChild = true

                innerStack.addAll(current.replacedChildren)
            }

            return hasRadioChild
        }

        setIf("role", SemanticsProperties.CollectionInfo) {
            if (areAllChildrenRadioButtons(node)) "radiogroup" else null
        }

        // https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Roles/textbox_role
        // https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input/text
        doIf(SemanticsProperties.IsEditable) {
            require(el is HTMLInputElement)
            el.setAttribute("type", "text")
            el.readOnly = node.config.getOrNull(SemanticsActions.SetText) == null
            setIf("aria-description", SemanticsProperties.InputText) { it.toString() }
        }

        for (event in listOf("keydown", "keyup")) el.addEventListener(event, EventListener {
            it.stopImmediatePropagation()
            it.stopPropagation()
            it.preventDefault()
            val x = js("new it.constructor(it.type, it);")
            canvas?.dispatchEvent(x)
        })

        setIf("role", SemanticsProperties.IsDialog) { "dialog" }

        doIf(SemanticsProperties.Text) { el.innerText = it.joinToString() }

        when (val onClick = node.config.getOrNull(SemanticsActions.OnClick)?.action) {
            null -> {
                if (clickListeners[node.id] != null) {
                    el.removeEventListener("click", clickListeners[node.id])
                    clickListeners.remove(node.id)
                }
            }

            else -> {
                if (clickListeners[node.id] == null) {
                    val clickListener = EventListener {
                        console.log("Click")
                        onClick()
                    }

                    el.addEventListener("click", clickListener)
                    clickListeners[node.id] = clickListener
                }
            }
        }


        // TODO: Logic
        // When either RequestFocus or Focused is set, the shadow dom element has to be focusable (e.g. via tabindex or similar)
        // On focus, we have to actually focus the shadow dom element for the screen reader to actually read the text
        // For this to properly work with the handlers from compose, we have to propagate keyboard events, the actual focus
        // event and click events back to the canvas or to the explicit handlers, if they are given.
        when (val requestFocus = node.config.getOrNull(SemanticsActions.RequestFocus)?.action) {
            null -> {
                if (listeners[node.id] != null) {
                    el.removeEventListener("focus", listeners[node.id])
                    listeners.remove(node.id)
                }
            }

            else -> {
                if (listeners[node.id] == null) {
                    val focusListener = EventListener {
                        console.log("Focus")
                        requestFocus()
                    }

                    el.addEventListener("focus", focusListener)
                    listeners[node.id] = focusListener
                }
            }
        }

        doIf(SemanticsProperties.Focused) { if (it) el.focus() }

        el.removeAttribute("aria-live")
        setIf("aria-live", SemanticsProperties.LiveRegion) {
            when (it) {
                LiveRegionMode.Polite -> "polite"
                LiveRegionMode.Assertive -> "assertive"
                else -> "off"
            }
        }

        val title = node.config.getOrNull(SemanticsProperties.PaneTitle)
        val description = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
        if (title != null) {
            el.setAttribute("title", title)
            if (title == "tooltip")
                el.setAttribute("role", "tooltip")

            if (description != null)
                el.setAttribute("aria-description", description)

        } else if (description != null) {
            el.setAttribute("aria-label", description)
        }
    }
}


fun <T> ItemArrayLike<T>.asSequence(): Sequence<T> = object : Sequence<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        var index = 0

        override fun next(): T = item(index).unsafeCast<T>().also { index++ }
        override fun hasNext(): Boolean = index < length

    }
}


fun onDomReady(block: () -> Unit) {
    if (document.readyState == DocumentReadyState.LOADING) {
        document.addEventListener("DOMContentLoaded", {
            block()
        })
    } else {
        block()
    }
}

fun AccessibleComposeViewport(content: @Composable () -> Unit = {}) {
    onDomReady {
        val body = document.body ?: error("failed to find <body> element")

        @OptIn(Connect2xComposeUiApi::class) ComposeViewport(
            viewportContainer = body,
            semanticsListener = { CanvasSemanticsOwnerListener(it) },
            configure = { },
            content = content,
        )
    }
}

@Serializable
@SerialName("simple/text")
object SimpleText : Route

@Serializable
@SerialName("navigator")
object Navigator

@Serializable
@SerialName("simple/button")
object SimpleButton : Route

@Serializable
@SerialName("simple/alert-dialog")
object SimpleAlertDialog : Route

sealed interface Route

@Composable
fun SimpleText() {
    Text("Simple Text")
}

@Composable
fun SimpleButton() {
    Button(onClick = { console.log("clicked") }) { Text("Simple Button 2") }
}


@Composable
inline fun <reified T : ViewModel> koinLifecycleAwareViewModel(
    qualifier: Qualifier? = null,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    viewModelStoreOwner: ViewModelStoreOwner = LocalViewModelStoreOwner.current
        ?: error("No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"),
    key: String? = null,
    extras: CreationExtras = defaultExtras(viewModelStoreOwner),
    scope: Scope = currentKoinScope(),
    noinline parameters: ParametersDefinition = { emptyParametersHolder() },
): T = koinViewModel(qualifier, viewModelStoreOwner, key, extras, scope) {
    parameters().add(lifecycleOwner)
}

@Composable
fun SimpleAlertDialog(
    state: DialogViewModel = koinLifecycleAwareViewModel()
) {
    Button(onClick = { state.dialogOpen = true }) { Text("Open Dialog") }

    if (state.dialogOpen) {
        AlertDialog(
            icon = { Icon(Icons.Default.AccessAlarm, "Alarm") },
            text = { Text("This is placeholder Text") },
            title = { Text("This is title text") },
            confirmButton = {
                TextButton(onClick = {
                    state.dialogOpen = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    state.dialogOpen = false
                }) { Text("Dismiss") }
            },
            onDismissRequest = { state.dialogOpen = false },
        )
    }
}

@Composable
fun Navigator(
    navigate: (Route) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Button(onClick = { navigate(SimpleText) }) { Text("Simple Text") }
        Button(onClick = { navigate(SimpleButton) }) { Text("Simple Button") }
        Button(onClick = { navigate(SimpleAlertDialog) }) { Text("Simple AlertDialog") }
    }
}

abstract class LifecycleAwareViewModel(private val lifecycleOwner: LifecycleOwner) : ViewModel(),
    DefaultLifecycleObserver {
    init {
        lifecycleOwner.lifecycle.addObserver(this)
        addCloseable(AutoCloseable {
            lifecycleOwner.lifecycle.removeObserver(this)
        })
    }
}

class DialogViewModel(
    myComponent: MyComponent,
    lifecycleOwner: LifecycleOwner,
    savedStateHandle: SavedStateHandle,
) : LifecycleAwareViewModel(lifecycleOwner) {
    var dialogOpen by savedStateHandle.saveable {
        mutableStateOf(localStorage.getItem("dialogOpen").toBoolean())
    }

    override fun onStop(owner: LifecycleOwner) {
        localStorage.setItem("dialogOpen", dialogOpen.toString())
    }
}

class MyComponent

@Module
class MyModule {

    @KoinViewModel
    fun dialog(
        myComponent: MyComponent,
        @InjectedParam lifecycleOwner: LifecycleOwner,
        @InjectedParam savedStateHandle: SavedStateHandle,
    ) = DialogViewModel(
        myComponent = myComponent, lifecycleOwner = lifecycleOwner, savedStateHandle = savedStateHandle
    )
}

@Module
class OtherModule {
    @Single
    fun myComponent() = MyComponent()
}

@Module(includes = [MyModule::class, OtherModule::class])
class CombinedModule

@KoinApplication(modules = [CombinedModule::class])
object SimpleApplication


@OptIn(ExperimentalMaterial3Api::class)
fun main2() = AccessibleComposeViewport {

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        Surface {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above
                    ), tooltip = {
                        PlainTooltip { Text("My Text") }
                    }, state = rememberTooltipState()
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.AccessAlarm, contentDescription = "Alarm Clock")
                    }
                }


            }
        }
    }

}

enum class Colors {
    Blue,
    Red,
    Green;

    override fun toString(): String = when (this) {
        Blue -> "Blue"
        Red -> "Red"
        Green -> "Green"
    }
}

fun Modifier.focusOnFirstRender(): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    var hasRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasRequested) {
            focusRequester.requestFocus()
            hasRequested = true
        }
    }

    this.focusRequester(focusRequester)
}


@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalBrowserHistoryApi::class
)
fun main() = AccessibleComposeViewport {
    CompositionLocalProvider(IsFocusHighlighting provides true) {
        ThemeImpl().create(
            messengerColors = ThemeDarkMessengerColorsImpl().create(md_theme_light_error),
            messengerDpConstants = DefaultMessengerDpConstantValues,
            messengerIcons = DefaultMessengerIcons,
            shapes = Shapes(),
            typography = Typography(),
            density = LocalDensity.current,
            componentStyles = ThemeComponentsImpl(),
            colorScheme = DefaultMessengerColorScheme
        ) {
            ThemedSurface(Modifier.fillMaxSize(), LocalComponentStyles.current.background) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column {


                        val l = listOf("a", "b", "c")
                        var v by remember { mutableStateOf(l[0]) }
                        ThemedSelect(
                            value = v,
                            onValueChange = { v = it },
                            options = l,
                            render = { it }
                        )

                        ThemedButton(onClick = { println("aaaaaaaaaaaaaaaaaaa") }) { Text(v) }

                        var selected by remember { mutableStateOf(false) }

                        val tfs = rememberTextFieldState("v")
                        OutlinedTextField(
                            state = tfs,
                            readOnly = selected,
                            lineLimits = TextFieldLineLimits.SingleLine,
                        )

                        ThemedListItemRadioButton(
                            headlineContent = { Text("textbox readonly") },
                            selected = selected,
                            onChange = { selected = !selected },
                            modifier = Modifier.focusOnFirstRender()
                        )


                        var selectedThemedListItemCheckbox by remember { mutableStateOf(false) }
                        ThemedListItemCheckbox(
                            headlineContent = { Text("Max mustermann") },
                            selected = selectedThemedListItemCheckbox,
                            onChange = { selectedThemedListItemCheckbox = !selectedThemedListItemCheckbox }
                        )

                        var openDialog by remember { mutableStateOf(false) }
                        ThemedListItemButton(
                            headlineContent = { Text("open dialog") },
                            onClick = { openDialog = true }
                        )

                        if (openDialog) ThemedAdaptiveDialog(onDismissRequest = { openDialog = false }) {
                            AdaptiveDialogHeader {
                                Text("the dialog header")
                            }
                            AdaptiveDialogContent {
                                Text("some content here")
                            }
                            AdaptiveDialogFooter {
                                ThemedButton({ openDialog = false }) { Text("dismiss") }
                                ThemedButton(
                                    { openDialog = false },
                                    style = MaterialTheme.components.primaryButton
                                ) { Text("accept") }
                            }
                        }

                        var switchChecked by remember { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()
                        var currentProgress by remember { mutableFloatStateOf(0f) }
                        ThemedListItemSwitch(
                            headlineContent = { Text("Start Loading") },
                            selected = switchChecked,
                            onChange = {
                                switchChecked = !switchChecked
                                scope.launch { loadProgress { currentProgress = it } }
                            },
                        )

                        if (switchChecked) ThemedProgressIndicator(
                            progress = { currentProgress },
                            style = MaterialTheme.components.linearProgressIndicator,
                            modifier = Modifier.semantics {
                                text = AnnotatedString((currentProgress * 100).toString() + "%")
                                liveRegion = LiveRegionMode.Assertive
                            },
                        )


                        var set by remember { mutableStateOf(Colors.Blue) }

                        Text(set.toString())

                        RadioSetting(
                            title = { Text(set.toString()) },
                            options = mapOf(
                                Colors.Blue to RadioSettingOption(Colors.Blue.toString()),
                                Colors.Red to RadioSettingOption(Colors.Red.toString()),
                                Colors.Green to RadioSettingOption(Colors.Green.toString()),
                            ),
                            value = set,
                            set = { set = it },
                        )


                        var checked by remember { mutableStateOf(false) }

                        Row {
                            Text("My Test")
                            Checkbox(checked, onCheckedChange = { checked = it }, Modifier.semantics() {
                                this.text = AnnotatedString("is duck")
                            })
                            Button(onClick = { checked = !checked }) { Text("Button") }
                        }

                        Tooltip(
                            tooltip = { Text("back") },
                            content = {
                                ThemedIconButton(
                                    onClick = { println("clicked") },
                                    content = { BanIcon() }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


suspend fun loadProgress(updateProgress: (Float) -> Unit) {
    for (i in 1..100) {
        updateProgress(i.toFloat() / 100)
        delay(100)
    }
}

//        val navController = rememberNavController()

//    KoinApplication(application = SimpleApplication.koinConfiguration()) {
//        Scaffold(
//            topBar = {
//                TopAppBar(title = {
//                    // Text("StoryBook")
//                }, navigationIcon = {
//                    val entry by navController.currentBackStackEntryAsState()
//                    val showBackButton = entry?.destination?.hasRoute<Navigator>() != true
//
//                    AnimatedVisibility(showBackButton) {
//                        TooltipBox(
//                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
//                                TooltipAnchorPosition.Above
//                            ),
//                            tooltip = {
//                                RichTooltip { Text("Return to Navigation") }
//                            },
//                            state = rememberTooltipState(false),
//                        ) {
//                            IconButton(onClick = { navController.popBackStack() }) {
//                                Icon(
//                                    Icons.AutoMirrored.Default.ArrowBack, "Return to Navigation"
//                                )
//                            }
//                        }
//                    }
//
//                })
//            }) {
//            Box(
//                contentAlignment = Alignment.Center,
//                modifier = Modifier.fillMaxSize(),
//            ) {
//                NavHost(
//                    navController = navController,
//                    startDestination = Navigator,
//                    enterTransition = { fadeIn(animationSpec = tween(100)) },
//                    exitTransition = { fadeOut(animationSpec = tween(100)) },
//                    popEnterTransition = { fadeIn(animationSpec = tween(100)) },
//                    popExitTransition = { fadeOut(animationSpec = tween(100)) },
//                ) {
//                    composable<SimpleText> { SimpleText() }
//                    composable<SimpleButton> { SimpleButton() }
//                    composable<SimpleAlertDialog> { SimpleAlertDialog() }
//                    composable<Navigator> {
//                        Navigator(
//                            navigate = { navController.navigate(it) })
//                    }
//                }
//
//                LaunchedEffect(navController) {
//                    navController.bindToBrowserNavigation()
//                }
//            }
//        }
//    }


//    MaterialExpressiveTheme(
//        colorScheme = colorScheme,
//    ) {
//        Surface {
//            Box(
//                contentAlignment = Alignment.Center,
//                modifier = Modifier.fillMaxSize(),
//            ) {
//                var opened by remember { mutableStateOf(false) }
//
//                Button(onClick = { opened = true }) { Text("Open") }
//
//                if (opened) {
//                    AlertDialog(
//                        icon = { Icon(Icons.Default.AddCard, "Add to Cart") },
//                        title = { Text("My Dialog") },
//                        text = { Text("This is a dialog") },
//                        onDismissRequest = { opened = false },
//                        confirmButton = {
//                            TextButton(onClick = { opened = false }) { Text("Confirm") }
//                        },
//                        dismissButton = {
//                            TextButton(onClick = { opened = false }) { Text("Dismiss") }
//                        },
//                    )
//                }
//            }
//        }
//    }
