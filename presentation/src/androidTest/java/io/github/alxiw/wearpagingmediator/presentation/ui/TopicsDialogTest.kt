package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.TopicsDialogState
import io.github.alxiw.wearpagingmediator.presentation.testing.awaitNode
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import io.github.alxiw.wearpagingmediator.presentation.testing.setWearContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards what the dialog of the topics shows in every state of its load. */
@RunWith(AndroidJUnit4::class)
class TopicsDialogTest {

    @get:Rule
    val rule = createComposeRule()

    private val repoName = "alxiw/wear-paging-mediator"
    private val description = "A Wear OS search sample that pages the results of GitHub through " +
        "a Room cache, so the cached results of a query are shown at once"
    private val stars = 468_224
    private val forks = 12_345

    @Test
    fun loadingDialogShowsTheRepoTheDescriptionAndTheLoadingState() {
        rule.setWearContent {
            TopicsDialog(
                state = TopicsDialogState.Loading(1, repoName, description, stars, forks),
                onDismiss = {},
                onTopicClick = {}
            )
        }

        rule.awaitNode(hasText(repoName))
        rule.onNodeWithText(description).assertIsDisplayed()
        rule.onNodeWithText(label(R.string.topics_loading)).assertIsDisplayed()
    }

    @Test
    fun loadedDialogShowsTheDescriptionAndListsEveryTopic() {
        rule.setWearContent {
            TopicsDialog(
                state = TopicsDialogState.Loaded(
                    repoId = 1,
                    repoName = repoName,
                    repoDescription = description,
                    repoStars = stars,
                    repoForks = forks,
                    topics = listOf("android", "paging", "wear-os")
                ),
                onDismiss = {},
                onTopicClick = {}
            )
        }

        rule.awaitNode(hasText(description))
        rule.onNodeWithText(description).assertIsDisplayed()
        rule.onNodeWithText("android").assertIsDisplayed()

        // the description takes the top of the dialog, the topics below it are scrolled to
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("wear-os"))
        rule.onNodeWithText("wear-os").assertIsDisplayed()
    }

    @Test
    fun dialogShowsTheStarsAndForksInFullBetweenTheDescriptionAndTheTopics() {
        rule.setWearContent {
            TopicsDialog(
                state = TopicsDialogState.Loaded(
                    repoId = 1,
                    repoName = repoName,
                    repoDescription = description,
                    repoStars = 1_468_224,
                    repoForks = 82_345,
                    topics = listOf("android")
                ),
                onDismiss = {},
                onTopicClick = {}
            )
        }

        rule.awaitNode(hasText(description))
        // the card of the list abbreviates the counts with K and M, the dialog never does
        rule.onNodeWithText("1468224").assertIsDisplayed()
        rule.onNodeWithText("82345").assertIsDisplayed()
        rule.onNodeWithText("1M").assertDoesNotExist()
        rule.onNodeWithContentDescription(label(R.string.stars)).assertExists()
        rule.onNodeWithContentDescription(label(R.string.forks)).assertExists()
    }

    @Test
    fun loadedDialogWithoutTopicsSaysThereAreNone() {
        rule.setWearContent {
            TopicsDialog(
                state = TopicsDialogState.Loaded(
                    repoId = 1,
                    repoName = repoName,
                    repoDescription = description,
                    repoStars = stars,
                    repoForks = forks,
                    topics = emptyList()
                ),
                onDismiss = {},
                onTopicClick = {}
            )
        }

        rule.awaitNode(hasText(label(R.string.topics_empty)))
        rule.onNodeWithText(description).assertIsDisplayed()
    }

    @Test
    fun failedDialogReportsTheFailure() {
        rule.setWearContent {
            TopicsDialog(
                state = TopicsDialogState.Failed(1, repoName, description, stars, forks),
                onDismiss = {},
                onTopicClick = {}
            )
        }

        rule.awaitNode(hasText(label(R.string.topics_error)))
        rule.onNodeWithText(description).assertIsDisplayed()
    }

    @Test
    fun dialogOfRepoWithoutDescriptionShowsTheStatsAndTheTopics() {
        rule.setWearContent {
            TopicsDialog(
                state = TopicsDialogState.Loaded(
                    repoId = 1,
                    repoName = repoName,
                    repoDescription = null,
                    repoStars = stars,
                    repoForks = forks,
                    topics = listOf("paging")
                ),
                onDismiss = {},
                onTopicClick = {}
            )
        }

        rule.awaitNode(hasText(repoName))
        rule.onNodeWithText("468224").assertIsDisplayed()
        rule.onNodeWithText("paging").assertIsDisplayed()
    }

    @Test
    fun tappedTopicIsReported() {
        val tapped = mutableListOf<String>()

        rule.setWearContent {
            TopicsDialog(
                state = TopicsDialogState.Loaded(
                    repoId = 1,
                    repoName = repoName,
                    repoDescription = null,
                    repoStars = stars,
                    repoForks = forks,
                    topics = listOf("android", "paging")
                ),
                onDismiss = {},
                onTopicClick = { tapped += it }
            )
        }

        rule.awaitNode(hasText("android"))
        rule.onNodeWithText("android").performClick()

        assertEquals(listOf("android"), tapped)
    }
}
