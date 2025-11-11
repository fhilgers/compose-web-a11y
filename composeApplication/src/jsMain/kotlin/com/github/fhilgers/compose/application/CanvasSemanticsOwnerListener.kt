@file:OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class, Connect2xComposeUiApi::class)

package com.github.fhilgers.compose.application

import androidx.compose.runtime.Composable
import androidx.compose.ui.Connect2xComposeUiApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import org.w3c.dom.*
import org.w3c.dom.events.EventListener
import kotlin.time.Duration.Companion.seconds

interface TestableSemanticsOwner {
    val rootSemanticsNode: TestableSemanticsNode
}

interface TestableSemanticsNode {
    val id: Int
    val config: SemanticsConfiguration
    val parent: TestableSemanticsNode?
    val replacedChildren: List<TestableSemanticsNode>
    val layoutInfo: LayoutInfo
    val boundsInRoot: Rect
}

interface TestableSemanticsOwnerListener {
    fun onSemanticsOwnerAppended(semanticsOwner: TestableSemanticsOwner)
    fun onSemanticsOwnerRemoved(semanticsOwner: TestableSemanticsOwner)
    fun onSemanticsChange(semanticsOwner: TestableSemanticsOwner)
    fun onLayoutChange(semanticsOwner: TestableSemanticsOwner, semanticsNodeId: Int)
}

class SemanticsOwnerWrapper(
    private val inner: SemanticsOwner,
) : TestableSemanticsOwner {
    override val rootSemanticsNode: TestableSemanticsNode
        get() = SemanticsNodeWrapper(inner.rootSemanticsNode)

}

class SemanticsNodeWrapper(
    private val inner: SemanticsNode,
) : TestableSemanticsNode {
    override val id: Int
        get() = inner.id


    override val config: SemanticsConfiguration
        get() = inner.config

    override val parent: TestableSemanticsNode?
        get() = inner.parent?.let(::SemanticsNodeWrapper)

    override val replacedChildren: List<TestableSemanticsNode>
        get() = inner.replacedChildren.map(::SemanticsNodeWrapper)

    override val layoutInfo: LayoutInfo
        get() = inner.layoutInfo

    override val boundsInRoot: Rect
        get() = inner.boundsInRoot
}

class TestableSemanticsOwnerListenerWrapper(
    private val inner: TestableSemanticsOwnerListener,
) : PlatformContext.SemanticsOwnerListener {
    override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) =
        inner.onSemanticsOwnerAppended(SemanticsOwnerWrapper(semanticsOwner))

    override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) =
        inner.onSemanticsOwnerRemoved(SemanticsOwnerWrapper(semanticsOwner))

    override fun onSemanticsChange(semanticsOwner: SemanticsOwner) =
        inner.onSemanticsChange(SemanticsOwnerWrapper(semanticsOwner))

    override fun onLayoutChange(
        semanticsOwner: SemanticsOwner,
        semanticsNodeId: Int
    ) = inner.onLayoutChange(SemanticsOwnerWrapper(semanticsOwner), semanticsNodeId)
}


@Suppress("FunctionName")
fun AccessibleComposeViewport(content: @Composable () -> Unit = {}) {
    onDomReady {
        val body = document.body ?: error("failed to find <body> element")

        ComposeViewport(
            viewportContainer = body,
            semanticsListener = { TestableSemanticsOwnerListenerWrapper(CanvasSemanticsOwnerListener(it)) },
            configure = { },
            content = content,
        )
    }
}

class CanvasSemanticsOwnerListener(
    val a11yContainer: HTMLDivElement,
    val coroutineScope: CoroutineScope = MainScope(),
) : TestableSemanticsOwnerListener {

    val owners = mutableSetOf<TestableSemanticsOwner>()

    val syncFlow =
        MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        a11yContainer.removeAttribute("aria-live")
        a11yContainer.setAttribute("role", "application")
        coroutineScope.launch {
            syncFlow
                .conflate()
                .collect {
                    for (owner in owners) {
                        onSemanticsChangeInner(owner)
                    }
            }
        }

        coroutineScope.launch {
            while (true) {
                syncFlow.emit(Unit)
                delay(2.seconds)
            }
        }
    }

    override fun onSemanticsOwnerAppended(semanticsOwner: TestableSemanticsOwner) {
        if (findElement(semanticsOwner.id) != null) return

        val ownerElement = document.createElement("div") as HTMLDivElement

        ownerElement.setAttribute("owner", "")
        ownerElement.setAttribute("semantics-id", semanticsOwner.rootSemanticsNode.id.toString())

        a11yContainer.appendChild(ownerElement)
        owners.add(semanticsOwner)
    }

    override fun onSemanticsOwnerRemoved(semanticsOwner: TestableSemanticsOwner) {
        val element = checkNotNull(findElement(semanticsOwner.id)) { "owner does not exist" }

        element.remove()
        owners.remove(semanticsOwner)
    }

    val listeners = mutableMapOf<Int, EventListener>()
    val clickListeners = mutableMapOf<Int, EventListener>()

    override fun onSemanticsChange(semanticsOwner: TestableSemanticsOwner) {
        syncFlow.tryEmit(Unit)
    }

    fun onSemanticsChangeInner(semanticsOwner: TestableSemanticsOwner) {
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

    override fun onLayoutChange(semanticsOwner: TestableSemanticsOwner, semanticsNodeId: Int) {
        return

//        fun inner(semanticsNodeId: Int) {
//            val node = findNode(semanticsNodeId, semanticsOwner.rootSemanticsNode) ?: return
//            val element = findElement(semanticsNodeId) ?: return
//
//            val rootPosition = a11yContainer.getBoundingClientRect().let {
//                Offset(it.left.toFloat(), it.top.toFloat())
//            }
//
//            val density = node.layoutInfo.density.density
//            val toRoot = node.layoutInfo.coordinates.localToRoot(rootPosition).div(density)
//            val size = node.boundsInRoot.size.div(density)
//
//            element.style.left = "${toRoot.x}px"
//            element.style.top = "${toRoot.y}px"
//            element.style.width = "${size.width}px"
//            element.style.height = "${size.height}px"
//
//            for (child in node.replacedChildren) inner(child.id)
//        }
//
//        coroutineScope.launch {
//            delay(100)
//            inner(semanticsOwner.id)
//        }
    }

    private fun findNode(
        semanticsId: Int, parent: TestableSemanticsNode
    ): TestableSemanticsNode? {
        if (parent.id == semanticsId) return parent
        for (child in parent.replacedChildren) return findNode(semanticsId, child) ?: continue
        return null
    }

    private fun findElement(
        semanticsId: Int, parent: HTMLElement = a11yContainer
    ): HTMLElement? {
        return parent.querySelector("""[semantics-id="$semanticsId"]""") as HTMLElement?
    }

    private fun collectIds(
        parent: HTMLElement = a11yContainer
    ): Set<Int> {
        return parent.querySelectorAll("""[semantics-id]""")
            .asSequence()
            .map { it as HTMLElement }
            .map { it.getAttribute("semantics-id")!!.toInt() }
            .toSet()
    }

    private fun HTMLElement.clearAll() {
        clear()

        for (attr in attributes.asSequence()
            .filter { it.name != "owner" && it.name != "semantics-id" && it.name != "style" }) removeAttributeNode(
            attr
        )
    }


    private val TestableSemanticsOwner.id: Int
        get() = rootSemanticsNode.id

    private fun basicHTMLElement(node: TestableSemanticsNode): HTMLElement {
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

    private fun setAttrs(el: HTMLElement, node: TestableSemanticsNode) {
        fun <T> setIf(attr: String, prop: SemanticsPropertyKey<T>, value: (T) -> String?) =
            node.config.getOrNull(prop)?.let {
                val v = value(it) ?: return@let null
                el.setAttribute(attr, v)
            }

        fun setIf(attr: String, prop: SemanticsPropertyKey<String>) = setIf(attr, prop) { it }

        fun <T> doIf(prop: SemanticsPropertyKey<T>, value: (T) -> Unit) =
            node.config.getOrNull(prop)?.let { value(it) }


        el.setAttribute("semantics-id", node.id.toString())
        el.style.position = "fixed"
        el.style.whiteSpace = "pre"

        setIf("data-test-tag", SemanticsProperties.TestTag)

        when (node.config.getOrNull(SemanticsProperties.Role)) {
            Role.DropdownList -> {
                // https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Roles/combobox_role
                el.setAttribute("role", "combobox")
                setIf("type", SemanticsProperties.IsEditable) { // text field
                    if (it) "text" else null
                }
                setIf("aria-expanded", SemanticsProperties.ToggleableState) {
                    when (it) {
                        ToggleableState.On -> "true"
                        ToggleableState.Off -> "false"
                        ToggleableState.Indeterminate -> null // better nothing than something wrong
                    }
                }
            }

            Role.RadioButton -> {
//                require(el is HTMLInputElement) { "Role.RadioButton is not HTMLInputElement" }
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
                node.config.getOrNull(SemanticsProperties.ToggleableState)?.let {
                    when (it) {
                        ToggleableState.On -> el.asDynamic().checked = true
                        ToggleableState.Off -> el.asDynamic().checked = false
                        ToggleableState.Indeterminate -> el.asDynamic().checked = false // I guess
                    }
                }
            }

            Role.Button -> {
                doIf(SemanticsProperties.Text) { el.innerText = it.joinToString() }
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
                setIf("aria-checked", SemanticsProperties.ToggleableState) {
                    when (it) {
                        ToggleableState.On -> "true"
                        ToggleableState.Off -> "false"
                        ToggleableState.Indeterminate -> "false"
                    }
                }
            }

            else -> {
                setIf("aria-label", SemanticsProperties.Text, { it.joinToString() })
                // https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Roles/textbox_role
                // https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input/text
                if (node.config.getOrNull(SemanticsProperties.IsEditable) != null && el is HTMLInputElement) {
                    el.setAttribute("type", "text")
                    setIf("aria-description", SemanticsProperties.InputText) { it.toString() }
                    el.removeAttribute("readonly")
                    if (node.config.getOrNull(SemanticsActions.SetText) == null)
                        el.setAttribute("readonly", "")
                }
            }
        }

        node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.let {
            require(el is HTMLProgressElement, { "node with ProgressBarRangeInfo is not HTMLProgressElement" })
            el.setAttribute("value", it.current.toString())
            el.setAttribute("max", it.range.endInclusive.toString())
        }

        fun areAllChildrenRadioButtons(node: TestableSemanticsNode): Boolean {
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

        for (event in listOf("keydown", "keyup")) el.addEventListener(event, EventListener {
            it.stopImmediatePropagation()
            it.stopPropagation()
            it.preventDefault()
            val x = js("new it.constructor(it.type, it);")
            canvas?.dispatchEvent(x)
        })

        setIf("role", SemanticsProperties.IsDialog) { "dialog" }

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
                    el.removeAttribute("tabindex")
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

                    if (el is HTMLDivElement)
                        el.setAttribute("tabindex", "-1")
                    el.addEventListener("focus", focusListener)
                    listeners[node.id] = focusListener
                }
            }
        }

        doIf(SemanticsProperties.Focused) { if (it) el.focus() }

        el.removeAttribute("aria-live")
        setIf("aria-live", SemanticsProperties.LiveRegion) {
            when (it) {
                LiveRegionMode.Companion.Polite -> "polite"
                LiveRegionMode.Companion.Assertive -> "assertive"
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

private fun <T> ItemArrayLike<T>.asSequence(): Sequence<T> = object : Sequence<T> {
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