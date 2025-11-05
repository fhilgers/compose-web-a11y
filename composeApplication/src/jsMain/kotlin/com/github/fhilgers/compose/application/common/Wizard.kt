package com.github.fhilgers.compose.application.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.fhilgers.compose.application.common.WizardButtons.NextButton
import com.github.fhilgers.compose.application.common.WizardNavigationButton.Custom
import com.github.fhilgers.compose.application.theme.components
import com.github.fhilgers.compose.application.theme.components.ThemedButton
import com.github.fhilgers.compose.application.theme.messengerDpConstants
import kotlin.properties.Delegates

typealias StepId = String

sealed interface WizardNavigationButton {
    data class Standard(
        val enabled: @Composable () -> Boolean = { true },
        val content: (@Composable () -> Unit)? = null
    ) : WizardNavigationButton

    data object None : WizardNavigationButton
    data class Custom(val button: @Composable CustomButtonScope.() -> Unit) : WizardNavigationButton
}

interface CustomButtonScope : RowScope {
    val currentStepId: MutableState<StepId>
    val nextStep: StepId?
    val previousStep: StepId?
}

private data class CustomButtonScopeImpl(
    val rowScope: RowScope,
    override val currentStepId: MutableState<StepId>,
    override val nextStep: StepId? = null,
    override val previousStep: StepId? = null,
) : RowScope by rowScope, CustomButtonScope

sealed interface WizardButtons {
    data object NextButton : WizardButtons
    data object AdditionalButton : WizardButtons
    data object BackButton : WizardButtons
}

@Immutable
data class WizardStep(
    val id: StepId,
    val title: @Composable () -> String,
    val content: @Composable (BoxWithConstraintsScope) -> Unit,
    val additionalButton: (@Composable RowScope.((StepId) -> Unit) -> Unit)? = null,
    val nextButton: (@Composable () -> WizardNavigationButton) = { WizardNavigationButton.Standard() },
    val backButton: (@Composable () -> WizardNavigationButton) = { WizardNavigationButton.Standard() },
    val buttonOrder: @Composable () -> Triple<WizardButtons, WizardButtons, WizardButtons> = {
        Triple(
            WizardButtons.AdditionalButton, WizardButtons.BackButton, NextButton
        )
    }
)

interface BackHandler {

    /**
     * Checks whether the provided [BackCallback] is registered or not.
     */
    fun isRegistered(callback: BackCallback): Boolean

    /**
     * Registers the specified [callback] to be called when the back button is invoked.
     */
    fun register(callback: BackCallback)

    /**
     * Unregisters the specified [callback].
     */
    fun unregister(callback: BackCallback)
}

abstract class BackCallback(
    isEnabled: Boolean = true,
    var priority: Int = PRIORITY_DEFAULT,
) {
    private var enabledListeners = emptySet<(Boolean) -> Unit>()

    /**
     * Controls the enabled state of the callback.
     */
    var isEnabled: Boolean by Delegates.observable(isEnabled) { _, _, newValue ->
        enabledListeners.forEach { it(newValue) }
    }

    /**
     * Registers the specified [listener] to be called when the enabled state of the callback changes.
     */
    fun addEnabledChangedListener(listener: (isEnabled: Boolean) -> Unit) {
        this.enabledListeners += listener
    }

    /**
     * Unregisters the specified [listener].
     */
    fun removeEnabledChangedListener(listener: (isEnabled: Boolean) -> Unit) {
        this.enabledListeners -= listener
    }

    /**
     * Called when the back button is pressed, or the predictive back gesture is finished.
     */
    abstract fun onBack()

    /**
     * Called when the predictive back gesture starts.
     */
    open fun onBackStarted(backEvent: BackEvent) {
    }

    /**
     * Called on every progress of the predictive back gesture.
     */
    open fun onBackProgressed(backEvent: BackEvent) {
    }

    /**
     * Called when the predictive back gesture is cancelled.
     */
    open fun onBackCancelled() {
    }

    companion object {
        const val PRIORITY_DEFAULT: Int = 0
        const val PRIORITY_MIN: Int = Int.MIN_VALUE
        const val PRIORITY_MAX: Int = Int.MAX_VALUE
    }
}

data class BackEvent(
    val progress: Float = 0F,
    val swipeEdge: SwipeEdge = SwipeEdge.UNKNOWN,
    val touchX: Float = 0F,
    val touchY: Float = 0F,
) {

    init {
        require(progress in 0F..1F) { "The 'progress' argument must be between 0 and 1 (both inclusive)" }
    }

    enum class SwipeEdge {
        UNKNOWN,
        LEFT,
        RIGHT,
    }
}

fun BackCallback(
    isEnabled: Boolean = true,
    priority: Int = 0,
    onBackStarted: ((BackEvent) -> Unit)? = null,
    onBackProgressed: ((BackEvent) -> Unit)? = null,
    onBackCancelled: (() -> Unit)? = null,
    onBack: () -> Unit,
): BackCallback =
    object : BackCallback(isEnabled = isEnabled, priority = priority) {
        override fun onBackStarted(backEvent: BackEvent) {
            onBackStarted?.invoke(backEvent)
        }

        override fun onBackProgressed(backEvent: BackEvent) {
            onBackProgressed?.invoke(backEvent)
        }

        override fun onBackCancelled() {
            onBackCancelled?.invoke()
        }

        override fun onBack() {
            onBack.invoke()
        }
    }

@Composable
fun Wizard(wizardSteps: List<WizardStep>, backHandler: BackHandler? = null) {
    val currentStepId = remember(wizardSteps) { mutableStateOf(wizardSteps.getOrNull(0)?.id ?: "unknown") }
    val savableStateHolder = rememberSaveableStateHolder()

    val wizardStep = wizardSteps.find { it.id == currentStepId.value }
    val previousStep = wizardSteps.getOrNull(wizardSteps.indexOf(wizardStep) - 1)?.id
    if (backHandler != null) {
        val onBack = rememberUpdatedState {
            previousStep?.let { currentStepId.value = it }
        }
        val callback = remember(onBack) {
            BackCallback(priority = 1) {
                onBack.value()
            }
        }
        DisposableEffect(backHandler, callback) {
            backHandler.register(callback)
            onDispose {
                backHandler.unregister(callback)
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        key(wizardStep) {
            if (wizardStep != null) {
                // this is necessary to have a scroll position saved on every step,
                // but not being linked (https://kotlinlang.slack.com/archives/CJLTWPH7S/p1715854224165609?thread_ts=1715852960.082249&cid=CJLTWPH7S)
                savableStateHolder.SaveableStateProvider(key = wizardStep.id) {
                    val scrollState = rememberScrollState()
                    Surface(
                        Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = MaterialTheme.messengerDpConstants.small)
                        ) {
                            BoxWithConstraints(
                                Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                WizardContainer(wizardSteps, wizardStep, currentStepId, scrollState)
                            }
                            // LegalFooter()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.WizardContainer(
    wizardSteps: List<WizardStep>,
    wizardStep: WizardStep,
    currentStep: MutableState<StepId>,
    scrollState: ScrollState,
) {
    val boxWithConstraints = this
    Surface(
        Modifier
            .widthIn(max = 800.dp)
            .heightIn(min = max(1200.dp, this.maxHeight))
            .padding(
                if (boxWithConstraints.maxWidth < 500.dp) MaterialTheme.messengerDpConstants.small
                else MaterialTheme.messengerDpConstants.large
            )
            .clip(RoundedCornerShape(MaterialTheme.messengerDpConstants.small)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        if (boxWithConstraints.maxWidth < 500.dp) MaterialTheme.messengerDpConstants.small
                        else MaterialTheme.messengerDpConstants.large
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column {
                    WizardHeading(wizardStep)
                    WizardContent(wizardStep, scrollState, boxWithConstraints)
                    WizardButtons(wizardSteps, wizardStep, currentStep)
                }
            }
//            VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
        }
    }
}

@Composable
private fun WizardHeading(wizardStep: WizardStep) {
    Text(
        wizardStep.title(),
        style = MaterialTheme.typography.titleLarge,
    )
    MiddleSpacer()
}

@Composable
private fun ColumnScope.WizardContent(
    wizardStep: WizardStep,
    scrollState: ScrollState,
    boxWithConstraints: BoxWithConstraintsScope,
) {
    Surface(
        Modifier.Companion
            .weight(1.0f, fill = true)
    ) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                wizardStep.content(boxWithConstraints)
            }
        }
    }
}

@Composable
private fun WizardButtons(
    wizardSteps: List<WizardStep>,
    wizardStep: WizardStep,
    currentStep: MutableState<StepId>,
) {
    val nextStep = wizardSteps.getOrNull(wizardSteps.indexOf(wizardStep) + 1)?.id
    val previousStep = wizardSteps.getOrNull(wizardSteps.indexOf(wizardStep) - 1)?.id
    val additionalButton = wizardStep.additionalButton
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Bottom) {
        val buttonList = wizardStep.buttonOrder().toList().toMutableList()
        if (additionalButton == null) {
            buttonList.remove(WizardButtons.AdditionalButton)
        }
        MessengerModalButtonRow(
            button1 = getCorrespondingButton(buttonList[0], wizardStep, nextStep, currentStep, previousStep),
            button2 = if (buttonList.size > 1)
                getCorrespondingButton(
                    buttonList[1],
                    wizardStep,
                    nextStep,
                    currentStep,
                    previousStep
                ) else null,
            button3 =
                if (buttonList.size > 2) getCorrespondingButton(
                    buttonList[2],
                    wizardStep,
                    nextStep,
                    currentStep,
                    previousStep
                ) else null

        )
    }
}

@Composable
private fun RowScope.NextButton(
    wizardStep: WizardStep,
    nextStep: StepId?,
    currentStep: MutableState<StepId>,
) {
    when (val nextButton = wizardStep.nextButton()) {
        is WizardNavigationButton.Standard -> {
            nextStep?.let { NextButtonImpl(currentStep, nextStep, nextButton) }
        }

        is WizardNavigationButton.None -> {}

        is Custom -> {
            nextButton.button(CustomButtonScopeImpl(this, currentStep, nextStep = nextStep))
        }
    }
}

@Composable
private fun NextButtonImpl(
    currentStep: MutableState<StepId>,
    nextStep: StepId,
    nextButton: WizardNavigationButton.Standard
) {
//    val i18n = DI.get<I18nView>()
    ThemedButton(
        style = MaterialTheme.components.primaryButton,
        onClick = { currentStep.value = nextStep },
        enabled = nextButton.enabled(),
    ) {
        if (nextButton.content != null) {
            nextButton.content()
        } else Text("i18n.commonNext()")
    }
}

@Composable
private fun RowScope.BackButton(wizardStep: WizardStep, currentStep: MutableState<StepId>, previousStep: StepId?) {
    return when (val backButton = wizardStep.backButton()) {
        is WizardNavigationButton.Standard -> {
            if (previousStep != null) {
                BackButtonImpl(currentStep, previousStep, backButton)
            } else {
            }
        }

        is WizardNavigationButton.None -> {}

        is Custom -> {
            backButton.button(CustomButtonScopeImpl(this, currentStep, previousStep = previousStep))
        }
    }
}

@Composable
private fun BackButtonImpl(
    currentStep: MutableState<StepId>,
    previousStep: StepId,
    backButton: WizardNavigationButton.Standard
) {
//    val i18n = DI.get<I18nView>()
    ThemedButton(
        style = MaterialTheme.components.commonButton,
        onClick = { currentStep.value = previousStep },
        enabled = backButton.enabled(),
    ) {
        if (backButton.content != null) {
            backButton.content()
        } else Text("i18n.commonBack()")
    }
}


@Composable
private fun getCorrespondingButton(
    wizardButton: WizardButtons,
    wizardStep: WizardStep,
    nextStep: StepId?,
    currentStep: MutableState<StepId>,
    previousStep: StepId?
): @Composable (RowScope.() -> Unit) {
    return when (wizardButton) {
        NextButton -> {
            {
                NextButton(wizardStep, nextStep, currentStep)
            }
        }

        WizardButtons.BackButton -> {
            {
                BackButton(wizardStep, currentStep, previousStep)
            }
        }

        WizardButtons.AdditionalButton -> {
            {
                wizardStep.additionalButton?.let { it { stepId -> currentStep.value = stepId } }
            }
        }
    }
}
