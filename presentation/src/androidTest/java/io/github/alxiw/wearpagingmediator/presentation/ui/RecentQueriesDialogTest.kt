package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.RecentQueriesDialogState
import io.github.alxiw.wearpagingmediator.presentation.testing.awaitNode
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import io.github.alxiw.wearpagingmediator.presentation.testing.setWearContent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.TimeZone

/** Guards what the dialog of the recent queries shows in every state of its load. */
@RunWith(AndroidJUnit4::class)
class RecentQueriesDialogTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var defaultZone: TimeZone

    @Before
    fun setUp() {
        // the moment of a query is formatted in the zone of the watch: the test fixes it, so the
        // expected timestamps do not depend on the machine the test runs on
        defaultZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultZone)
    }

    @Test
    fun loadingDialogNamesTheNumberOfQueriesAndShowsTheLoad() {
        rule.setWearContent {
            RecentQueriesDialog(
                state = RecentQueriesDialogState.Loading,
                onDismiss = {},
                onQueryClick = {}
            )
        }

        // the title states what the dialog is for: the last MAX_CACHED_QUERIES used queries
        val title = label(R.string.recent_queries_title, RecentQuery.MAX_CACHED_QUERIES)
        rule.awaitNode(hasText(title))
        rule.onNodeWithText(title).assertIsDisplayed()
        rule.onNodeWithText(label(R.string.recent_queries_loading)).assertIsDisplayed()
    }

    @Test
    fun loadedDialogShowsEveryQueryWithTheMomentItWasUsed() {
        rule.setWearContent {
            RecentQueriesDialog(
                state = RecentQueriesDialogState.Loaded(
                    listOf(
                        recent("compose", "2026-10-21T17:16:56Z"),
                        recent("android wear", "2026-10-20T09:05:43Z")
                    )
                ),
                onDismiss = {},
                onQueryClick = {}
            )
        }

        rule.awaitNode(hasText("compose"))
        rule.onNodeWithText("17:16:56 21.10.2026").assertIsDisplayed()

        // the title takes the top of the dialog, the queries below it are scrolled to
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("android wear"))
        rule.onNodeWithText("09:05:43 20.10.2026").assertIsDisplayed()
    }

    @Test
    fun loadedDialogWithoutQueriesSaysThereAreNone() {
        rule.setWearContent {
            RecentQueriesDialog(
                state = RecentQueriesDialogState.Loaded(emptyList()),
                onDismiss = {},
                onQueryClick = {}
            )
        }

        rule.awaitNode(hasText(label(R.string.recent_queries_empty)))
    }

    @Test
    fun failedDialogReportsTheFailure() {
        rule.setWearContent {
            RecentQueriesDialog(
                state = RecentQueriesDialogState.Failed,
                onDismiss = {},
                onQueryClick = {}
            )
        }

        rule.awaitNode(hasText(label(R.string.recent_queries_error)))
    }

    @Test
    fun tappedQueryIsReported() {
        val tapped = mutableListOf<String>()

        rule.setWearContent {
            RecentQueriesDialog(
                state = RecentQueriesDialogState.Loaded(
                    listOf(recent("kotlin", "2026-10-21T17:16:56Z"))
                ),
                onDismiss = {},
                onQueryClick = { tapped += it }
            )
        }

        rule.awaitNode(hasText("kotlin"))
        rule.onNodeWithText("kotlin").performClick()

        assertEquals(listOf("kotlin"), tapped)
    }

    private fun recent(raw: String, instant: String) = RecentQuery(
        query = SearchQuery.of(raw),
        accessedAt = Instant.parse(instant)
    )
}
