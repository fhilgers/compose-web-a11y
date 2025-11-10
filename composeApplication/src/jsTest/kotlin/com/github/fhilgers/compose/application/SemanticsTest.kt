package com.github.fhilgers.compose.application

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Connect2xComposeUiApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class SemanticsTest {

    // TODO maybe a dsl like this?
//    @Test
//    fun aaa() = semTest2 {
//        ui {
//            Button(
//                onClick = {},
//                modifier = Modifier.semantics { testTag = "t-btn" },
//                content = { Text("aaa") },
//            )
//        }
//
//        val btn = a11yRoot.byTestTag("t-btn") as HTMLButtonElement
//        println(btn.outerHTML)
//    }

    @Test
    fun aa() = semTest({
        Button(
            onClick = {},
            modifier = Modifier.semantics { testTag = "t-btn" },
            content = { Text("aaa") },
        )
    }) { a11yRoot ->
        val btn = a11yRoot.byTestTag("t-btn") as HTMLButtonElement
        println(btn.outerHTML)
    }

    @Test
    fun radiogroup() = semTest({
        Column(
            modifier = Modifier.semantics {
                testTag = "t-group"
                collectionInfo = CollectionInfo(3, 1)
            }) {
            RadioButton(selected = true, onClick = {})
            RadioButton(selected = false, onClick = {})
            RadioButton(selected = false, onClick = {})
        }
    }) { a11yRoot ->
        val radioGroup = a11yRoot.byTestTag("t-group") as HTMLDivElement
        assertEquals("radiogroup", radioGroup.attributes["role"]?.value)
        println(radioGroup.outerHTML)
    }
}

@OptIn(Connect2xComposeUiApi::class, ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
private fun semTest(
    content: @Composable () -> Unit,
    assertions: suspend (Element) -> Unit,
) = runTest {
    val root = document.createElement("div")
    document.body?.appendChild(root)

    onDomReady {
        ComposeViewport(
            viewportContainer = root,
            semanticsListener = { CanvasSemanticsOwnerListener(it, backgroundScope) },
            content = content,
        )
    }

    var a11yRoot: Element? = null
    waitUntil {
        a11yRoot = root.shadowRoot?.getElementById("cmp_a11y_root")
        a11yRoot != null
    }

    assertions(a11yRoot ?: error("could not find cmp_a11y_root"))

    document.body?.removeChild(root)
}

private suspend fun waitUntil(condition: () -> Boolean) {
    while (!condition()) delay(50.milliseconds)
}

private fun Element.byTestTag(tag: String): Element? = this.querySelector("[data-test-tag='$tag']")