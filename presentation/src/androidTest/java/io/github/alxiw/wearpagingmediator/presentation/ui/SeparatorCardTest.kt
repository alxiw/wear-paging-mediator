package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.testing.pluralLabel
import io.github.alxiw.wearpagingmediator.presentation.testing.setWearContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards the label of a star band and the separator that is shown for it. */
@RunWith(AndroidJUnit4::class)
class SeparatorCardTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun millionCapIsAbbreviatedInBandLabel() {
        rule.setWearContent {
            SeparatorCard(cap = 1_000_000)
        }

        rule.onNodeWithText(pluralLabel(R.plurals.stars_label, 1_000_000, "1M")).assertIsDisplayed()
    }

    @Test
    fun thousandCapIsAbbreviatedInBandLabel() {
        rule.setWearContent {
            SeparatorCard(cap = 500_000)
        }

        rule.onNodeWithText(pluralLabel(R.plurals.stars_label, 500_000, "500K")).assertIsDisplayed()
    }

    @Test
    fun smallCapIsShownAsIsInBandLabel() {
        rule.setWearContent {
            SeparatorCard(cap = 100)
        }

        rule.onNodeWithText(pluralLabel(R.plurals.stars_label, 100, "100")).assertIsDisplayed()
    }

    @Test
    fun singleStarCapUsesSingularForm() {
        rule.setWearContent {
            SeparatorCard(cap = 1)
        }

        rule.onNodeWithText(pluralLabel(R.plurals.stars_label, 1, "1")).assertIsDisplayed()
    }
}
