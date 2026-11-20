package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import io.github.alxiw.wearpagingmediator.presentation.testing.setWearContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards that the head of the list always shows the query the results belong to. */
@RunWith(AndroidJUnit4::class)
class SearchButtonTest {

    @get:Rule
    val rule = createComposeRule()

    private var clicks = 0
    private var longClicks = 0

    @Test
    fun buttonShowsTheQueryAndAsksForANewOne() {
        rule.setWearContent {
            SearchButton(query = "kotlin", onClick = { clicks++ })
        }

        rule.onNodeWithText("kotlin").assertIsDisplayed()
        rule.onNodeWithContentDescription(label(R.string.search)).assertExists()

        rule.onNodeWithText("kotlin").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun longPressAsksForTheRecentQueries() {
        rule.setWearContent {
            SearchButton(
                query = "kotlin",
                onClick = { clicks++ },
                onLongClick = { longClicks++ },
                onLongClickLabel = label(R.string.recent_queries_label)
            )
        }

        rule.onNodeWithText("kotlin").performTouchInput { longClick() }

        assertEquals(1, longClicks)
        assertEquals(0, clicks)
    }

    @Test
    fun buttonShowsTheDefaultQueryAsIs() {
        rule.setWearContent {
            SearchButton(query = DEFAULT_QUERY.value, onClick = {})
        }

        rule.onNodeWithText(DEFAULT_QUERY.value).assertIsDisplayed()
    }
}
