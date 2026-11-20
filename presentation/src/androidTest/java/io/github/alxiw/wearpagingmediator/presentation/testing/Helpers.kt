package io.github.alxiw.wearpagingmediator.presentation.testing

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.material3.MaterialTheme

/** How long a test waits for the results of a query to be reported. */
internal const val TIMEOUT_MILLIS = 5_000L

/** The string of the resources of the module, in the language the test runs in. */
internal fun label(@StringRes resId: Int, vararg args: Any): String =
    ApplicationProvider.getApplicationContext<Context>().getString(resId, *args)

/** The plural string of the resources of the module, in the language the test runs in. */
internal fun pluralLabel(@PluralsRes resId: Int, count: Int, vararg args: Any): String =
    ApplicationProvider.getApplicationContext<Context>().resources
        .getQuantityString(resId, count, *args)

/** Shows [content] the way the app does: in the theme of the module. */
internal fun ComposeContentTestRule.setWearContent(content: @Composable () -> Unit) {
    setContent {
        MaterialTheme {
            content()
        }
    }
}

/** Waits until a node matching [matcher] is in the tree. */
internal fun ComposeTestRule.awaitNode(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = TIMEOUT_MILLIS
) {
    waitUntil(timeoutMillis) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
}

/** Waits until [condition] holds, e.g. until the data layer was asked for the next query. */
internal fun ComposeTestRule.awaitCondition(
    timeoutMillis: Long = TIMEOUT_MILLIS,
    condition: () -> Boolean
) {
    waitUntil(timeoutMillis, condition)
}
