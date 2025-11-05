package com.github.fhilgers.compose.application.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.fhilgers.compose.application.common.icons.HelpIcon
import com.github.fhilgers.compose.application.theme.components
import com.github.fhilgers.compose.application.theme.components.ThemedIconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.launch

//@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
open class TextFieldViewModelImpl private constructor(
    private val delegate: MutableStateFlow<TextFieldViewModel.State>,
    maxLength: Int,
) : TextFieldViewModel, StateFlow<TextFieldViewModel.State> by delegate.asStateFlow() {
    constructor(
        maxLength: Int,
        initialText: String = "",
        initialSelection: IntRange? = null,
    ) : this(MutableStateFlow(TextFieldViewModel.State(initialText, initialSelection, 1UL)), maxLength)

    override val text: Flow<String>
        get() = map { it.text }.distinctUntilChanged()
    override val textValue: String
        get() = value.text
    override val selection: Flow<IntRange?>
        get() = map { it.selection }.distinctUntilChanged()
    override val selectionValue: IntRange?
        get() = value.selection
    override val maxLength: Int = maxLength

    override fun update(text: String, selection: IntRange?, epoch: ULong?) {
        delegate.update {
            if (epoch == null || epoch > it.epoch) {
                TextFieldViewModel.State(
                    epoch = it.epoch + 1u,
                    text = text.take(maxLength),
                    selection = selection?.let {
                        selection.first.coerceIn(0..maxLength)..selection.last.coerceIn(0..maxLength)
                    },
                )
            } else {
//                log.trace { "skip update, because epoch $epoch > ${it.epoch}" }
                it
            }
        }
    }
}

interface TextFieldViewModel : StateFlow<TextFieldViewModel.State> {
    /**
     * Represents the state of the text field, including the text content and selection range.
     *
     * @property text The current text in the text field.
     * @property selection The range of the current selection, or `null` if no selection exists.
     * @property epoch The current epoch of the State. A higher epoch means, that the state one is likely newer than the other.
     *           This is useful to prevent unnecessary updates in the UI (depending on the implementation).
     */
    class State(
        val text: String,
        selection: IntRange?,
        val epoch: ULong,
    ) {
        val selection: IntRange? = selection?.coerceIn(text)

        private fun IntRange.coerceIn(text: String): IntRange =
            if (text.isEmpty()) IntRange(0, 0)
            else {
                val firstWithinLimits = first.coerceIn(0..text.length)
                val lastWithinLimits = last.coerceIn(firstWithinLimits..text.length)
                IntRange(firstWithinLimits, lastWithinLimits)
            }

        operator fun component1() = text
        operator fun component2() = selection
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as State

            if (epoch != other.epoch) return false
            if (text != other.text) return false
            if (selection != other.selection) return false

            return true
        }

        override fun hashCode(): Int {
            var result = epoch.hashCode()
            result = 31 * result + text.hashCode()
            result = 31 * result + (selection?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "State(epoch=$epoch, text='$text', selection=$selection)"
        }
    }

    /**
     * Use this if you are interested in [State.text] only. Otherwise, use [value].
     */
    val text: Flow<String>

    /**
     * Use this if you are interested in [State.text] only. Otherwise, use [value].
     */
    val textValue: String

    /**
     * Use this if you are interested in [State.selection] only. Otherwise, use [value].
     */
    val selection: Flow<IntRange?>

    /**
     * Use this if you are interested in [State.selection] only. Otherwise, use [value].
     */
    val selectionValue: IntRange?

    /**
     * The maximum allowed characters in the text field. Everything above the limit will be cut.
     */
    val maxLength: Int

    /**
     * Update the state.
     */
    fun update(text: String, selection: IntRange? = null, epoch: ULong? = null)
}

interface ApprovableTextFieldViewModel : TextFieldViewModel {
    val isEdit: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    fun startEdit()
    fun cancelEdit()
    fun approveEdit()
}

class ApprovableTextFieldViewModelImpl(
    serverValue: Flow<String?>,
    maxLength: Int,
    private val coroutineScope: CoroutineScope,
    private val onApplyChange: suspend (String) -> Result<*>,
) : TextFieldViewModelImpl(maxLength), ApprovableTextFieldViewModel {
    private val serverStateValue = serverValue
        .map { it ?: "" }
        .stateIn(coroutineScope, Eagerly, "")

    private val _isEditing = MutableStateFlow(false)
    override val isEdit: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()

    init {
        coroutineScope.launch {
            serverStateValue.collect {
                if (isLoading.value || isEdit.value.not()) {
                    forceSetText(it)
                    _isLoading.value = false
                }
                _error.value = null
            }
        }
    }

    override fun startEdit() {
        _isEditing.value = true
        _error.value = null
    }

    override fun cancelEdit() {
        _isEditing.value = false
        _error.value = null
        forceSetText(serverStateValue.value)
    }

    override fun approveEdit() {
        val newValue = value.text
        if (newValue != serverStateValue.value && _isLoading.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                _isLoading.value = true
                onApplyChange(newValue)
                    .onFailure {
                        _isLoading.value = false
                        _error.value = it.message
                    }.onSuccess {
                        _isEditing.value = false
                        _error.value = null
                    }
            }
        }
    }

    private fun forceSetText(text: String) {
        super.update(text, null, null)
    }

    override fun update(text: String, selection: IntRange?, epoch: ULong?) {
        if (_isEditing.value) {
            super.update(text, selection, epoch)
        } else {
//            log.trace { "prevent update of value because isEditing is false" }
        }
    }
}

private fun TextFieldViewModel.State.toTextFieldValue(maxLength: Int): TextFieldValue {
    return TextFieldValue(
        text.take(maxLength),
        selection?.run {
            TextRange(first, last.coerceIn(0..text.length))
        } ?: TextRange.Zero
    )
}


@Composable
fun TextFieldViewModel.collectAsTextFieldValueState(focusRequester: FocusRequester? = null): MutableState<TextFieldValue> {
    val uiEpoch = remember { mutableStateOf(0UL) }
    val uiState = remember {
        val delegate = mutableStateOf(value.toTextFieldValue(maxLength))
        object : MutableState<TextFieldValue> by delegate {
            override var value: TextFieldValue
                get() = delegate.value
                set(value) {
                    delegate.value = value.copy(value.text.take(maxLength))
                }
        }
    }
    val uiStateValue = uiState.value

    // UI -> VM sync
    LaunchedEffect(uiStateValue) {
        uiEpoch.value++
        if (uiEpoch.value > value.epoch) {
            update(uiStateValue.text, uiStateValue.selection.run { IntRange(start, end) }, uiEpoch.value)
        }
    }

    // VM -> UI sync
    LaunchedEffect(Unit) {
        collect { vmState ->
            if (vmState.epoch > uiEpoch.value) {
                val newState = vmState.toTextFieldValue(maxLength)

                if (newState != uiState.value) {
                    // If the state has changed, the UI -> VM sync will increment the epoch, so we need to compensate for that
                    uiEpoch.value = vmState.epoch - 1u
                    uiState.value = newState
                } else {
                    // If the state is the same, the UI -> VM sync won't run, so we must set the epoch directly
                    uiEpoch.value = vmState.epoch
                }
                focusRequester?.requestFocus()
            }
        }
    }

    return uiState
}

@Composable
fun ApprovableTextField(
    viewModel: ApprovableTextFieldViewModel,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    textCaption: String = "",
    textPlaceholder: String = "",
    textInfoCannotChange: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
//    val i18n = DI.get<I18nView>()
    val isEdit by viewModel.isEdit.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var value by viewModel.collectAsTextFieldValueState()
    val interactionSource = remember { MutableInteractionSource() }
    val hasFocus = interactionSource.collectIsFocusedAsState().value

    LaunchedEffect(hasFocus) {
        if (hasFocus && !isEdit) {
            viewModel.startEdit()
        }
    }

    Column {
        Text(text = textCaption, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(10.dp))
        when {
            isLoading -> LoadingSpinner()

            isEditable -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isEdit) {
                        Tooltip({ Text("i18n.commonCancel()") }) {
                            ThemedIconButton(
                                style = MaterialTheme.components.commonIconButton,
                                onClick = viewModel::cancelEdit,
                            ) {
                                Icon(Icons.Outlined.Clear, "i18n.commonCancel()")
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                    }
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        enabled = isEdit,
                        placeholder = { Text(textPlaceholder) },
                        modifier = modifier
                            .weight(1.0f, fill = true)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = {
                                    if (!isEdit) viewModel.startEdit()
                                })
                            }
                            .focusable(enabled = true, interactionSource = interactionSource),
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.surfaceTint,
                        ),
                        keyboardOptions = keyboardOptions,
                    )
                    if (isEdit) {
                        Spacer(Modifier.size(10.dp))
                        Tooltip({ Text("i18n.commonRename()") }) {
                            ThemedIconButton(
                                style = MaterialTheme.components.commonIconButton,
                                onClick = viewModel::approveEdit,
                            ) {
                                Icon(Icons.Default.Check, "i18n.commonRename()")
                            }
                        }
                    }
                }
            }

            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        enabled = false,
                    )
                    HelpIcon(textInfoCannotChange)
                }
            }
        }
    }
}
