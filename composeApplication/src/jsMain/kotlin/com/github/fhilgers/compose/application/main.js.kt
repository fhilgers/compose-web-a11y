@file:OptIn(
    InternalComposeUiApi::class, ExperimentalComposeUiApi::class,
)

package com.github.fhilgers.compose.application

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Connect2xComposeUiApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.onImeAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.bindToBrowserNavigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import org.koin.compose.KoinApplication
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
import org.koin.ksp.generated.koinConfiguration
import org.koin.viewmodel.defaultExtras
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.ItemArrayLike
import org.w3c.dom.LOADING
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.KeyboardEvent


class CanvasSemanticsOwnerListener(
    val a11yContainer: HTMLDivElement,
    val coroutineScope: CoroutineScope = MainScope(),
) : PlatformContext.SemanticsOwnerListener {

    val focusTask = Channel<() -> Unit>(capacity = 1, BufferOverflow.DROP_OLDEST)

    init {
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

//            console.log(node.config.map { it.key.name }.toJsArray())

            val role = node.config.getOrNull(SemanticsProperties.Role)

            val element = findElement(node.id) ?: run {
//                console.log("Creating new element for ${node.id} as it does not exist yet")
                val element = if (role == Role.Button) {
                    val el = document.createElement("button") as HTMLButtonElement
                    el.setAttribute("type", "button")
                    el
                } else {
                    document.createElement("div") as HTMLDivElement
                }

                val canvas = a11yContainer.previousElementSibling?.previousElementSibling as? HTMLCanvasElement
                element.addEventListener("keydown", EventListener {
                    it.stopImmediatePropagation()
                    it.stopPropagation()
                    it.preventDefault()
                    val x = js("new it.constructor(it.type, it);")
                    canvas?.dispatchEvent(x)
                });
                element.addEventListener("keyup", EventListener {
                    it.stopImmediatePropagation()
                    it.stopPropagation()
                    it.preventDefault()
                    val x = js("new it.constructor(it.type, it);")
                    canvas?.dispatchEvent(x)
                });

                val parentElement = node.parent?.id?.let(::findElement) ?: a11yContainer

//                console.log("Parent element of ${node.id} is ${parentElement.getAttribute("semantics-id")}")

                element.setAttribute("semantics-id", node.id.toString())
                element.style.position = "fixed"
                element.style.whiteSpace = "pre"

                val nextElement = node.parent?.let {
                    val index = it.children.indexOf(node).takeIf { it >= 0 } ?: return@let null
                    it.children.getOrNull(index + 1)?.id?.let(::findElement)
                }

//                console.log("Element after this element is ${nextElement?.getAttribute("semantics-id")}")

                if (nextElement != null) {
//                    console.log("Inserting element before ${nextElement.getAttribute("semantics-id")}")
                    parentElement.insertBefore(element, nextElement)
                } else {
//                    console.log("Inserting element at the end")
                    parentElement.appendChild(element)
                }

                element
            }

            onLayoutChange(semanticsOwner, node.id)

            // element.clearAll()

            when (role) {
                Role.Button -> element.setAttribute("role", "button")
                Role.Image -> element.setAttribute("role", "img")
            }



            val isDialog = node.config.getOrNull(SemanticsProperties.IsDialog)

            if (isDialog != null) {
                element.setAttribute("role", "dialog")
                element.setAttribute("aria-modal", "true")
            }

            val text = node.config.getOrNull(SemanticsProperties.Text)

            if (text != null) element.innerText = text.joinToString()

            val onClick = node.config.getOrNull(SemanticsActions.OnClick)?.action

            if (onClick != null && clickListeners[node.id] == null) {

                val clickListener = EventListener {
                    console.log("Click")
                    onClick()
                }

                element.addEventListener("click", clickListener)
                clickListeners[node.id] = clickListener
            } else if (onClick == null && clickListeners[node.id] != null) {
                element.removeEventListener(
                    "click", clickListeners[node.id]
                )
                clickListeners.remove(node.id)
            }

            val requestFocus = node.config.getOrNull(SemanticsActions.RequestFocus)?.action

            if (requestFocus != null && listeners[node.id] == null) {
                element.setAttribute("tabindex", "0")
                val focusListener = EventListener {
                    // console.log("Focus", document.activeElement)
                    console.log("Focus")
                    // requestFocus()
                };
                element.addEventListener("focus", focusListener)
                listeners[node.id] = focusListener
            } else if (requestFocus == null && listeners[node.id] != null) {
                element.removeEventListener(
                    "focus", listeners[node.id]
                )
                listeners.remove(node.id)
            }

            val focussed = node.config.getOrNull(SemanticsProperties.Focused)

            if (focussed == true) {
                element.focus()
            }

            val title = node.config.getOrNull(SemanticsProperties.PaneTitle)
            val description =
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            node.config.getOrNull(SemanticsProperties.IsPopup)

            if (title != null) {
                if (title == "tooltip") {
                    element.setAttribute("role", "tooltip")
                } else {
                    element.setAttribute("aria-label", title)
                }
                if (description != null) {
                    element.setAttribute("aria-description", description)
                }
            } else if (description != null) {
                element.setAttribute("aria-label", description)
            }


            queue.addAll(node.children)
        }

        val unseen = currentIds - seen

        for (id in unseen) {
            findElement(id)?.remove()
            listeners.remove(id)
        }
    }

    override fun onLayoutChange(
        semanticsOwner: SemanticsOwner, semanticsNodeId: Int
    ) {

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

            for (child in node.children) inner(child.id)
        }

        inner(semanticsNodeId)
        // coroutineScope.launch {
        //     delay(100)
        // }
    }

    private fun findNode(
        semanticsId: Int, parent: SemanticsNode
    ): SemanticsNode? {
        if (parent.id == semanticsId) return parent

        for (child in parent.children) return findNode(semanticsId, child) ?: continue

        return null
    }

    private fun findElement(
        semanticsId: Int, parent: HTMLElement = a11yContainer
    ): HTMLElement? {
        if (parent.getAttribute("semantics-id")?.toInt() == semanticsId) return parent

        for (child in parent.children.asSequence()
            .filterIsInstance<HTMLElement>()) return findElement(semanticsId, child) ?: continue

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

        @OptIn(Connect2xComposeUiApi::class)
        ComposeViewport(
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
        myComponent = myComponent,
        lifecycleOwner = lifecycleOwner,
        savedStateHandle = savedStateHandle
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
            )  {

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above
                    ),
                    tooltip = {
                        PlainTooltip { Text("My Text") }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick =  {}) {
                        Icon(Icons.Default.AccessAlarm, contentDescription = "Alarm Clock")
                    }
                }

            }
        }
    }

}


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalBrowserHistoryApi::class
)
fun main() = AccessibleComposeViewport {
    KoinApplication(
        application = SimpleApplication.koinConfiguration(),
    ) {
        val navController = rememberNavController()

        MaterialTheme(
            colorScheme = colorScheme,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(title = {
                        // Text("StoryBook")
                    }, navigationIcon = {
                        val entry by navController.currentBackStackEntryAsState()
                        val showBackButton = entry?.destination?.hasRoute<Navigator>() != true

                        AnimatedVisibility(showBackButton) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = {
                                    RichTooltip { Text("Return to Navigation") }
                                },
                                state = rememberTooltipState(false),
                            ) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Default.ArrowBack, "Return to Navigation"
                                    )
                                }
                            }
                        }

                    })
                }) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Navigator,
                        enterTransition = {
                            fadeIn(animationSpec = tween(100))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(100))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(100))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(100))
                        },
                    ) {
                        composable<SimpleText> { SimpleText() }
                        composable<SimpleButton> { SimpleButton() }
                        composable<SimpleAlertDialog> { SimpleAlertDialog() }
                        composable<Navigator> {
                            Navigator(
                                navigate = { navController.navigate(it) })
                        }
                    }

                    LaunchedEffect(navController) {
                        navController.bindToBrowserNavigation()
                    }
                }
            }
        }
    }


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
}