package com.github.fhilgers.compose.application

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Connect2xComposeUiApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.window.Dialog
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.w3c.dom.Element
import org.w3c.dom.asList
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalMaterial3Api
class SemanticsTest {

    // TODO maybe create a dsl like this to define variables for use in both ui and assertions
//    @Test
//    fun aaa() = semTest2 {
//        val tag = "t-btn"
//        ui {
//            Button(
//                onClick = {},
//                modifier = Modifier.semantics { testTag = tag },
//                content = { Text("aaa") },
//            )
//        }
//
//        val btn = a11yRoot.byTestTag(tag) as HTMLButtonElement
//    }

    @Test
    fun `correct button element and inner text`() = a11yTest({
        Button(
            onClick = {},
            modifier = Modifier.semantics { testTag = "t-btn" },
            content = { Text("aaa") },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("t-btn"),
            tag = "button",
            innerHTML = "aaa",
        )
    }

    @Test
    fun `collection containing only RadioButtons is a radiogroup`() = a11yTest({
        Column(Modifier.semantics {
            testTag = "t-group"
            collectionInfo = CollectionInfo(3, 1)
        }) {
            RadioButton(selected = true, onClick = {})
            RadioButton(selected = false, onClick = {})
            RadioButton(selected = false, onClick = {})
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("t-group"),
            attrs = mapOf("role" to "radiogroup"),
        )
    }

    @Test
    fun `editable dropdown is text input`() = a11yTest({
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                    .semantics {
                        testTag = "dd"
                        if (expanded)
                            collapse { expanded = false; true }
                        else
                            expand { expanded = true; true }
                    },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem({ Text("a") }, {})
                DropdownMenuItem({ Text("b") }, {})
                DropdownMenuItem({ Text("c") }, {})
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("dd"),
            tag = "input",
            attrs = mapOf(
                "type" to "text",
                "role" to "combobox",
                "aria-expanded" to "false"
            ),
        )
    }

    @Test
    fun `readonly dropdown is button`() = a11yTest({
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                state = rememberTextFieldState(),
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .semantics {
                        testTag = "dd"
                        if (expanded)
                            collapse { expanded = false; true }
                        else
                            expand { expanded = true; true }
                    },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem({ Text("a") }, {})
                DropdownMenuItem({ Text("b") }, {})
                DropdownMenuItem({ Text("c") }, {})
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("dd"),
            tag = "button",
            attrs = mapOf(
                "role" to "combobox",
                "aria-expanded" to "false"
            ),
        )
    }

    @Test
    fun `expanded dropdown is expanded`() = a11yTest({
        var expanded by remember { mutableStateOf(true) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                state = rememberTextFieldState(),
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .semantics {
                        testTag = "dd"
                        if (expanded)
                            collapse { expanded = false; true }
                        else
                            expand { expanded = true; true }
                    },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem({ Text("a") }, {})
                DropdownMenuItem({ Text("b") }, {})
                DropdownMenuItem({ Text("c") }, {})
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("dd"),
            tag = "button",
            attrs = mapOf(
                "role" to "combobox",
                "aria-expanded" to "true"
            ),
        )
    }

    @Test
    fun `checkbox is input type checkbox`() = a11yTest({
        Checkbox(
            checked = false,
            onCheckedChange = {},
            modifier = Modifier.semantics { testTag = "cb" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("cb"),
            tag = "input",
            attrs = mapOf(
                "type" to "checkbox",
            ),
        )
    }

    @Test
    fun `checked checkbox is checked`() = a11yTest({
        Checkbox(
            checked = true,
            onCheckedChange = {},
            modifier = Modifier.semantics { testTag = "cb" },
        )
    }) { a11yRoot ->
        val cb = a11yRoot.byTestTag("cb")
        assertElem(cb, "input", mapOf("type" to "checkbox"))
        assertTrue(cb.asDynamic().checked, "checkbox not checked")
    }

    @Test
    fun `switch is button role switch`() = a11yTest({
        Switch(
            checked = false,
            onCheckedChange = {},
            modifier = Modifier.semantics { testTag = "switch" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("switch"),
            tag = "button",
            attrs = mapOf(
                "role" to "switch",
                "aria-checked" to "false",
            ),
        )
    }

    @Test
    fun `checked switch is checked`() = a11yTest({
        Switch(
            checked = true,
            onCheckedChange = {},
            modifier = Modifier.semantics { testTag = "switch" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("switch"),
            tag = "button",
            attrs = mapOf(
                "role" to "switch",
                "aria-checked" to "true",
            ),
        )
    }

    @Test
    fun `semantics text switch is aria label`() = a11yTest({
        Switch(
            checked = false,
            onCheckedChange = {},
            modifier = Modifier.semantics {
                text = AnnotatedString("lorem")
                testTag = "switch"
            },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("switch"),
            tag = "button",
            attrs = mapOf(
                "role" to "switch",
                "aria-checked" to "false",
                "aria-label" to "lorem"
            ),
        )
    }

    @Test
    fun `radio button is input type radio`() = a11yTest({
        RadioButton(
            selected = false,
            onClick = {},
            modifier = Modifier.semantics { testTag = "rbtn" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("rbtn"),
            tag = "input",
            attrs = mapOf("type" to "radio"),
        )
    }

    @Test
    fun `checked radio button is checked`() = a11yTest({
        RadioButton(
            selected = true,
            onClick = {},
            modifier = Modifier.semantics { testTag = "rbtn" },
        )
    }) { a11yRoot ->
        val rb = a11yRoot.byTestTag("rbtn")
        assertElem(
            elem = rb,
            tag = "input",
            attrs = mapOf("type" to "radio"),
        )
        assertTrue(rb.asDynamic().checked, "radio button not checked")
    }

    @Test
    fun `radio button semantics text is aria label`() = a11yTest({
        RadioButton(
            selected = false,
            onClick = {},
            modifier = Modifier.semantics {
                text = AnnotatedString("rbtn text")
                testTag = "rbtn"
            },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("rbtn"),
            tag = "input",
            attrs = mapOf("type" to "radio", "aria-label" to "rbtn text"),
            innerHTML = "",
        )
    }

    @Test
    fun `text field is input type text`() = a11yTest({
        TextField(
            state = rememberTextFieldState("lorem"),
            modifier = Modifier.semantics { testTag = "tf" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("tf"),
            tag = "input",
            attrs = mapOf(
                "type" to "text",
                "aria-description" to "lorem",
            ),
            innerHTML = "",
        )
    }

    @Test
    fun `readonly text field readonly`() = a11yTest({
        TextField(
            state = rememberTextFieldState("lorem"),
            readOnly = true,
            modifier = Modifier.semantics { testTag = "tf" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("tf"),
            tag = "input",
            attrs = mapOf(
                "type" to "text",
                "aria-description" to "lorem",
                "readonly" to "",
            ),
            innerHTML = "",
        )
    }

    @Test
    fun `dialog has role dialog`() = a11yTest({
        Dialog(onDismissRequest = {}) {
            Box(modifier = Modifier.semantics(true) { testTag = "dg" }) {
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("dg")?.parentElement,
            tag = "div",
            attrs = mapOf("role" to "dialog"),
        )
    }

    @Test
    fun `progress bar is progress`() = a11yTest({
        LinearProgressIndicator(
            progress = { 0.5F },
            modifier = Modifier.semantics { testTag = "pb" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("pb"),
            tag = "progress",
            attrs = mapOf(
                "value" to "0.5",
                "max" to "1",
            ),
        )
    }

    @Test
    fun `progress bar range info is progress`() = a11yTest({
        Box(
            modifier = Modifier.semantics {
                testTag = "pb"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = 0.5f,
                    range = 0f..1f,
                )
            },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("pb"),
            tag = "progress",
            attrs = mapOf(
                "value" to "0.5",
                "max" to "1",
            ),
        )
    }

    @Test
    fun `labeled progress bar is has label`() = a11yTest({
        LinearProgressIndicator(
            progress = { 0.5F },
            modifier = Modifier.semantics {
                testTag = "pb"
                text = AnnotatedString("lorem ipsum")
            },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("pb"),
            tag = "progress",
            attrs = mapOf(
                "value" to "0.5",
                "max" to "1",
                "aria-label" to "lorem ipsum",
            ),
        )
    }

    @Test
    fun `live region is aria live`() = a11yTest({
        Text(
            text = "Lorem ipsum",
            modifier = Modifier.semantics {
                testTag = "t"
                liveRegion = LiveRegionMode.Assertive
            },
        )
    }) { a11yRoot ->
        assertElem(
            a11yRoot.byTestTag("t"),
            tag = "div",
            attrs = mapOf(
                "aria-live" to "assertive",
                "aria-label" to "Lorem ipsum",
            ),
        )
    }

    @Test
    fun `tooltip is role tooltip`() = a11yTest({
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            state = rememberTooltipState(),
            modifier = Modifier.semantics {
                testTag = "tt"
                paneTitle = "tooltip"
            },
            tooltip = { Text("tooltiptext") },
        ) {
            Button(
                onClick = {},
                content = { Text("btn text") },
            )
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("tt"),
            tag = "div",
            attrs = mapOf(
                "role" to "tooltip",
                "title" to "tooltip",
            ),
        )
    }

    @Test
    fun `button opening popup menu is expandable `() = a11yTest({
        var expanded by remember { mutableStateOf(false) }
        Button(
            onClick = { expanded != expanded },
            modifier = Modifier.semantics {
                testTag = "btn"
                if (expanded) collapse { expanded = false; true }
                else expand { expanded = true; true }
            },
        ) {
            Text("Open me")
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem({ Text("a") }, {})
                DropdownMenuItem({ Text("b") }, {})
                DropdownMenuItem({ Text("c") }, {})
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("btn"),
            tag = "button",
            attrs = mapOf("aria-expanded" to "false"),
            innerHTML = "Open me",
        )
    }
}

@OptIn(Connect2xComposeUiApi::class, ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
private fun a11yTest(
    content: @Composable () -> Unit,
    assertions: suspend (Element) -> Unit,
) = runTest {
    val root = document.createElement("div")
    document.body?.appendChild(root)

    onDomReady {
        ComposeViewport(
            viewportContainer = root,
            semanticsListener = {
                TestableSemanticsOwnerListenerWrapper(
                    CanvasSemanticsOwnerListener(
                        it,
                        backgroundScope
                    )
                )
            },
            content = content,
        )
    }

    var a11yRoot: Element? = null
    waitUntil {
        a11yRoot = root.shadowRoot?.getElementById("cmp_a11y_root")
        a11yRoot != null
    }

//    delay(10.seconds)
    println(a11yRoot?.innerHTML)
    assertions(a11yRoot ?: error("could not find cmp_a11y_root"))

    document.body?.removeChild(root)
}

private suspend fun waitUntil(condition: () -> Boolean) {
    while (!condition()) delay(50.milliseconds)
}

private fun Element.byTestTag(tag: String): Element? = this.querySelector("[data-test-tag='$tag']")

private fun assertAttr(el: Element, attr: String, expected: String) = assertEquals(expected, el.attributes[attr]?.value)

private fun assertAttrs(el: Element?, attrs: Map<String, String>?) {
    val actualAttrs = el?.attributes?.asList()?.associate { Pair(it.name, it.value) }?.toMutableMap()
    for (key in listOf("semantics-id", "style", "data-test-tag"))
        actualAttrs?.remove(key)
    assertEquals(attrs, actualAttrs, "wrong attributes")
}

private fun assertElem(
    elem: Element?,
    tag: String? = null,
    attrs: Map<String, String>? = null,
    innerHTML: String? = null,
) {
    if (tag != null)
        assertEquals(tag.lowercase(), elem?.tagName?.lowercase(), "wrong tag")
    if (attrs != null)
        assertAttrs(elem, attrs)
    if (innerHTML != null)
        assertEquals(innerHTML, elem?.innerHTML, "wrong inner html")
}
