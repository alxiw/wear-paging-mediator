package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import io.github.alxiw.wearpagingmediator.presentation.testing.setWearContent
import io.github.alxiw.wearpagingmediator.presentation.util.UiText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards the label of a star band and the separator that is shown for it. */
@RunWith(AndroidJUnit4::class)
class SeparatorCardTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun bandLabelIsResolvedFromTheResources() {
        rule.setWearContent {
            SeparatorCard(description = UiText.StringResource(R.string.stars_label, "500K"))
        }

        rule.onNodeWithText(label(R.string.stars_label, "500K")).assertIsDisplayed()
    }

    @Test
    fun bandWithoutStarsHasItsOwnLabel() {
        rule.setWearContent {
            SeparatorCard(description = UiText.StringResource(R.string.stars_zero))
        }

        rule.onNodeWithText(label(R.string.stars_zero)).assertIsDisplayed()
    }

    @Test
    fun dynamicLabelIsShownAsIs() {
        rule.setWearContent {
            SeparatorCard(description = UiText.DynamicString("42 stars"))
        }

        rule.onNodeWithText("42 stars").assertIsDisplayed()
    }
}
