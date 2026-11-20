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
import io.github.alxiw.wearpagingmediator.presentation.util.TextShortener
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

    /**
     * GitHub does not bound a description: a result of the `android` search carries 55700
     * characters, and a Text lays such a text out in full even when it shows only a few lines of
     * it, which froze the app for seconds while scrolling.
     */
    @Test
    fun descriptionGitHubDoesNotBoundIsShownOnlyUpToTheLimitOfTheCard() {
        val unbound = "Book list of the collection ".repeat(2_000) // 54000 characters
        val spammedRepo = repo(id = 8, name = "some-many-books", description = unbound)

        rule.setWearContent {
            RepoCard(repo = spammedRepo, onClick = {})
        }

        rule.onNodeWithText("some-many-books").assertIsDisplayed()
        rule.onNodeWithText(TextShortener.shorten(unbound)).assertIsDisplayed()
        // the whole paragraph never reaches the Text of the card, it would be laid out in full
        rule.onNodeWithText(unbound).assertDoesNotExist()
    }
}
