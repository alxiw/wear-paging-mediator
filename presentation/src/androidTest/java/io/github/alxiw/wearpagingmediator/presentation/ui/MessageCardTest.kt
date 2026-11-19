package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import io.github.alxiw.wearpagingmediator.presentation.testing.setWearContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards the short message shown instead of the results. */
@RunWith(AndroidJUnit4::class)
class MessageCardTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun emptySearchIsReportedAsSuch() {
        rule.setWearContent {
            MessageCard(text = label(R.string.empty))
        }

        rule.onNodeWithText(label(R.string.empty)).assertIsDisplayed()
    }

    @Test
    fun endOfTheListIsReportedAfterTheLastResult() {
        rule.setWearContent {
            MessageCard(text = label(R.string.end_of_list))
        }

        rule.onNodeWithText(label(R.string.end_of_list)).assertIsDisplayed()
    }
}
