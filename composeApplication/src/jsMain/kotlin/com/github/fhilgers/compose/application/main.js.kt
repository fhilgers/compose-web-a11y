@file:OptIn(
    InternalComposeUiApi::class, ExperimentalComposeUiApi::class,
)

package com.github.fhilgers.compose.application

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import org.w3c.dom.ItemArrayLike
import org.w3c.dom.LOADING

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
