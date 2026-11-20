package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.RecentQueriesDialogState
import io.github.alxiw.wearpagingmediator.presentation.util.TimestampFormatter
import java.time.Instant

/**
 * The dialog with the most recently used queries, opened by a long press on the search button. The
 * title names the number of the queries from [RecentQuery.MAX_CACHED_QUERIES], every entry shows
 * its query and the moment it was used, and a tap reports the query through [onQueryClick] to
 * search for it, exactly as a tap on a topic does.
 *
 * The last shown state is kept while the dialog animates out: a dismissed dialog is still composed
 * for a moment, and without it the content would disappear before the animation ends.
 */
@Composable
fun RecentQueriesDialog(
    state: RecentQueriesDialogState?,
    onDismiss: () -> Unit,
    onQueryClick: (String) -> Unit
) {
    var lastShown by remember { mutableStateOf<RecentQueriesDialogState?>(null) }
    SideEffect {
        if (state != null) lastShown = state
    }

    val shown = state ?: lastShown ?: return

    AlertDialog(
        visible = state != null,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.recent_queries_title,
                    RecentQuery.MAX_CACHED_QUERIES
                )
            )
        },
        content = {
            when (shown) {
                is RecentQueriesDialogState.Loading -> item {
                    Text(text = stringResource(R.string.recent_queries_loading))
                }
                is RecentQueriesDialogState.Loaded -> if (shown.queries.isEmpty()) {
                    item { Text(text = stringResource(R.string.recent_queries_empty)) }
                } else {
                    items(shown.queries) { recent ->
                        RecentQueryCard(
                            recent = recent,
                            onClick = { onQueryClick(recent.query.value) }
                        )
                    }
                }
                is RecentQueriesDialogState.Failed -> item {
                    Text(text = stringResource(R.string.recent_queries_error))
                }
            }
        }
    )
}

/**
 * A recent query in its own outline, like a topic: the query takes up to two lines, the moment it
 * was used sits below it in the quieter label style, so the two entries of the same search can be
 * told apart. The card is a button: a tap searches for its query.
 */
@Composable
private fun RecentQueryCard(recent: RecentQuery, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = recent.query.value,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
            Text(
                text = TimestampFormatter.format(recent.accessedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun RecentQueryCardPreview() {
    MaterialTheme {
        AppScaffold {
            ScreenScaffold {
                TransformingLazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { Spacer(modifier = Modifier.height(65.dp)) }
                    item {
                        RecentQueryCard(
                            recent = RecentQuery(
                                query = SearchQuery.of("compose"),
                                accessedAt = Instant.parse("2026-10-21T17:16:56Z")
                            ),
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}
