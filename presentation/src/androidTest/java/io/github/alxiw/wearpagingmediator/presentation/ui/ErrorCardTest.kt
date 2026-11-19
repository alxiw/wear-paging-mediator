package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import io.github.alxiw.wearpagingmediator.presentation.testing.setWearContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards that a failed load is reported together with a way to retry it. */
@RunWith(AndroidJUnit4::class)
class ErrorCardTest {

    @get:Rule
    val rule = createComposeRule()

    private var retries = 0

    @Test
    fun cardShowsTheFailureAndRetriesIt() {
        rule.setWearContent {
            ErrorCard(message = label(R.string.error_refresh), onRetry = { retries++ })
        }

        rule.onNodeWithText(label(R.string.error_refresh)).assertIsDisplayed()

        rule.onNodeWithContentDescription(label(R.string.retry), useUnmergedTree = true)
            .performClick()

        assertEquals(1, retries)
    }

    @Test
    fun appendFailureIsReportedWithItsOwnMessage() {
        rule.setWearContent {
            ErrorCard(message = label(R.string.error_append), onRetry = {})
        }

        rule.onNodeWithText(label(R.string.error_append)).assertIsDisplayed()
    }
}
