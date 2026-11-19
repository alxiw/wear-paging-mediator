package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.testing.FIRST_REPO
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import io.github.alxiw.wearpagingmediator.presentation.testing.repo
import io.github.alxiw.wearpagingmediator.presentation.testing.setWearContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards what a result of a search shows and that it opens its repository. */
@RunWith(AndroidJUnit4::class)
class RepoCardTest {

    @get:Rule
    val rule = createComposeRule()

    private var clicks = 0

    @Test
    fun cardShowsTheRepoAndOpensIt() {
        rule.setWearContent {
            RepoCard(repo = FIRST_REPO, onClick = { clicks++ })
        }

        rule.onNodeWithText(FIRST_REPO.name).assertIsDisplayed()
        rule.onNodeWithText(FIRST_REPO.description!!).assertIsDisplayed()
        rule.onNodeWithText(FIRST_REPO.language!!).assertIsDisplayed()

        // the counts are abbreviated on the card, e.g. 1200000 stars read as "1M"
        rule.onNodeWithText("1M").assertIsDisplayed()
        rule.onNodeWithText("50K").assertIsDisplayed()

        rule.onNodeWithText(FIRST_REPO.name).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun statsStayReadableForAScreenReader() {
        rule.setWearContent {
            Box(modifier = Modifier.fillMaxSize()) {
                RepoCard(repo = FIRST_REPO, onClick = {})
            }
        }

        rule.onNodeWithContentDescription(label(R.string.stars)).assertExists()
        rule.onNodeWithContentDescription(label(R.string.forks)).assertExists()
    }

    @Test
    fun repoWithoutDescriptionAndLanguageShowsItsNameAndStats() {
        val bareRepo = repo(
            id = 7,
            name = "bare-repo",
            description = null,
            language = null,
            stars = 90,
            forks = 12
        )

        rule.setWearContent {
            RepoCard(repo = bareRepo, onClick = {})
        }

        rule.onNodeWithText("bare-repo").assertIsDisplayed()
        rule.onNodeWithText("90").assertIsDisplayed()
        rule.onNodeWithText("12").assertIsDisplayed()
    }
}
