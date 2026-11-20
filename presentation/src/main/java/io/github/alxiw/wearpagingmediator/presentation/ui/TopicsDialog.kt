package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.TopicsDialogState
import io.github.alxiw.wearpagingmediator.presentation.util.CountFormatter

/**
 * The dialog with the topics of a tapped result: the name of the repo, its description in full,
 * its stars and forks in full and then the topics. The topics are read when it opens, so it shows
 * the loading state first. A topic is a way to search for it: tapping one reports it through
 * [onTopicClick].
 *
 * The last shown state is kept while the dialog animates out: a dismissed dialog is still composed
 * for a moment, and without it the content would disappear before the animation ends.
 */
@Composable
fun TopicsDialog(
    state: TopicsDialogState?,
    onDismiss: () -> Unit,
    onTopicClick: (String) -> Unit
) {
    var lastShown by remember { mutableStateOf<TopicsDialogState?>(null) }
    SideEffect {
        if (state != null) lastShown = state
    }

    val shown = state ?: lastShown ?: return
    // the card of the list shortens a description (see TextShortener), the dialog shows it in full
    val description = shown.repoDescription?.takeIf { it.isNotBlank() }

    AlertDialog(
        visible = state != null,
        onDismissRequest = onDismiss,
        title = {
            Text(text = shown.repoName)
        },
        content = {
            if (description != null) {
                item {
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // the card of the list abbreviates the counts, the dialog shows the real numbers
            item {
                RepoStats(repoStars = shown.repoStars, repoForks = shown.repoForks)
            }
            when (shown) {
                is TopicsDialogState.Loading -> item {
                    Text(text = stringResource(R.string.topics_loading))
                }
                is TopicsDialogState.Loaded -> if (shown.topics.isEmpty()) {
                    item { Text(text = stringResource(R.string.topics_empty)) }
                } else {
                    items(shown.topics) { topic ->
                        TopicCard(topic = topic, onClick = { onTopicClick(topic) })
                    }
                }
                is TopicsDialogState.Failed -> item {
                    Text(text = stringResource(R.string.error_topics))
                }
            }
        }
    )
}

/**
 * A topic in its own outline, so a list of them reads as separate labels and the bounds of a long
 * list are clear. The card is a button: a tap searches for its topic.
 */
@Composable
private fun TopicCard(topic: String, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.height(42.dp)
    ) {
        Text(
            text = topic,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
    }
}

/**
 * The stars and forks of the repo in full: the card of the list abbreviates them (see
 * [CountFormatter]), the dialog shows the real numbers behind them. The pair sits between the
 * description and the topics, so it reads as a fact about the repo, not as one of its topics.
 */
@Composable
private fun RepoStats(repoStars: Int, repoForks: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_star),
            contentDescription = stringResource(R.string.stars),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = CountFormatter.formatFull(repoStars),
            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
        Icon(
            painter = painterResource(R.drawable.ic_fork),
            contentDescription = stringResource(R.string.forks),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = CountFormatter.formatFull(repoForks),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun TopicCardPreview() {
    MaterialTheme {
        AppScaffold {
            ScreenScaffold {
                TransformingLazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { Spacer(modifier = Modifier.height(65.dp)) }
                    item { TopicCard(topic = "android", onClick = {}) }
                }
            }
        }
    }
}

