package io.github.alxiw.wearpagingmediator.presentation.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY
import io.github.alxiw.wearpagingmediator.presentation.MainViewModel
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.ListStatus
import io.github.alxiw.wearpagingmediator.presentation.model.MainEffect
import io.github.alxiw.wearpagingmediator.presentation.model.MainIntent
import io.github.alxiw.wearpagingmediator.presentation.model.MainState
import io.github.alxiw.wearpagingmediator.presentation.model.ListModel
import io.github.alxiw.wearpagingmediator.presentation.model.TopicsDialogState
import io.github.alxiw.wearpagingmediator.presentation.ui.preview.PreviewLoadStates
import io.github.alxiw.wearpagingmediator.presentation.ui.preview.WearPreview
import io.github.alxiw.wearpagingmediator.presentation.ui.preview.rememberPreviewPagingItems

/**
 * The connected part of the screen: it owns the state holder, the launcher of the voice panel and
 * the effects of the ViewModel, and renders [MainScreenContent] with the state it gets back.
 *
 * The results of the query stay on screen in every load state and look the same whether they come
 * from the cache or from the network: a load state is only reported next to the items ([ListStatus]),
 * so the list is never replaced by a spinner and the cached results of a query are shown right away.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val listState = rememberTransformingLazyColumnState()
    val context = LocalContext.current

    val repos: LazyPagingItems<ListModel> = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    val state by viewModel.state.collectAsState()
    // the effects are collected once, so they read the paging items of the current frame
    val currentRepos by rememberUpdatedState(repos)

    // Back resets a custom search back to the default query. On the default query there is
    // nothing to reset, so leave the event to the system and let it finish the activity.
    BackHandler(enabled = state.query != DEFAULT_QUERY) {
        viewModel.onIntent(MainIntent.Search(DEFAULT_QUERY))
    }

    // A new query (including the reset done by back) always starts the results from the top.
    // The query is read before the suspending scroll, so the scroll is reported for the query
    // it was actually made for.
    LaunchedEffect(state.query, state.scrollToTopPending) {
        if (state.scrollToTopPending) {
            val query = state.query
            listState.scrollToItem(0)
            viewModel.onIntent(MainIntent.ScrolledToTop(query))
        }
    }

    // Not a part of the state: a refresh of the same query and an opened link are consumed once.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MainEffect.RefreshResults -> currentRepos.refresh()
                // a watch may have nothing to open a link with, so a failure to hand the link
                // over must not bring the screen down with it
                is MainEffect.OpenUrl -> if (!context.openLink(effect.url)) {
                    Toast.makeText(context, R.string.error_open_link, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val searchLabel = stringResource(R.string.search_label)
    val launcher = rememberLauncherForActivityResult(WearSearchContract()) { query ->
        Log.d("HELLO", "QUERY: $query")
        viewModel.onIntent(MainIntent.QuerySubmitted(query))
    }

    MainScreenContent(
        state = state,
        repos = repos,
        onSearchClick = { launcher.launch(searchLabel) },
        onRepoClick = { repo ->
            Log.d("HELLO", "Repo ${repo.name} clicked")
            viewModel.onIntent(MainIntent.RepoClicked(repo))
        },
        onRepoLongClick = { repo -> viewModel.onIntent(MainIntent.RepoLongClicked(repo)) },
        onTopicsDismiss = { viewModel.onIntent(MainIntent.TopicsDialogDismissed) },
        onRetry = repos::retry,
        listState = listState
    )
}

/**
 * The stateless part of the screen: it renders [state] and reports the events of the list up, so
 * the same content is shown in the previews for every load state of the results.
 */
@Composable
internal fun MainScreenContent(
    state: MainState,
    repos: LazyPagingItems<ListModel>,
    onSearchClick: () -> Unit,
    onRepoClick: (Repo) -> Unit,
    onRepoLongClick: (Repo) -> Unit,
    onTopicsDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    listState: TransformingLazyColumnState = rememberTransformingLazyColumnState()
) {
    // What is reported instead of the results (there are none yet) and right after the last item.
    val loadState = repos.loadState
    val notice = ListStatus.notice(loadState.refresh, loadState.append, repos.itemCount)
    val footer = ListStatus.footer(loadState.refresh, loadState.append, repos.itemCount)
    val linkLabel = stringResource(R.string.open_link_label)

    AppScaffold(modifier = modifier) {
        ScreenScaffold(
            scrollState = listState,
        ) { contentPadding ->
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                // search button
                item {
                    SearchButton(
                        query = state.query.value,
                        onClick = onSearchClick
                    )
                }

                // the query has nothing to show yet: the head of the list is never taken by a
                // state of the load, a refresh of the results that are already shown is not
                // reported here at all
                notice?.let { noticeItem ->
                    item { ListNoticeItem(noticeItem) }
                }

                // repos items
                items(
                    count = repos.itemCount,
                    key = repos.itemKey { it.stableKey },
                    contentType = repos.itemContentType { it.contentType }
                ) { index ->
                    when (val item = repos[index]) {
                        // a tap opens the topics of the result, a long press hands its link over
                        is ListModel.RepoItem -> RepoCard(
                            repo = item.repo,
                            onClick = { onRepoClick(item.repo) },
                            onLongClick = { onRepoLongClick(item.repo) },
                            onLongClickLabel = linkLabel
                        )
                        is ListModel.SeparatorItem -> SeparatorCard(description = item.description)
                        null -> Unit // Placeholder for loading
                    }
                }

                // load state below the loaded results, a failed refresh included
                footer?.let { footerItem ->
                    item { ListFooterItem(footer = footerItem, onRetry = onRetry) }
                }
            }
        }

        // the dialog lives in the scaffold to dim and scale the screen behind it
        TopicsDialog(state = state.topicsDialog, onDismiss = onTopicsDismiss)
    }
}

@Composable
private fun WearScreenPreview(content: @Composable () -> Unit) {
    MaterialTheme { content() }
}

private val PREVIEW_QUERY = SearchQuery.of("compose")

@WearPreview
@Composable
private fun MainScreenLoadedPreview() {
    WearScreenPreview {
        MainScreenContent(
            state = MainState(query = PREVIEW_QUERY),
            repos = rememberPreviewPagingItems(),
            onSearchClick = {},
            onRepoClick = {},
            onRepoLongClick = {},
            onTopicsDismiss = {},
            onRetry = {}
        )
    }
}

@WearPreview
@Composable
private fun MainScreenTopicsPreview() {
    WearScreenPreview {
        MainScreenContent(
            state = MainState(
                query = PREVIEW_QUERY,
                topicsDialog = TopicsDialogState.Loaded(
                    repoId = 1,
                    repoName = "alxiw/wear-paging-mediator",
                    repoDescription = "A Wear OS search sample that pages the results of GitHub " +
                        "through a Room cache, so the cached results of a query are shown at once",
                    topics = listOf("android", "paging", "wear-os")
                )
            ),
            repos = rememberPreviewPagingItems(),
            onSearchClick = {},
            onRepoClick = {},
            onRepoLongClick = {},
            onTopicsDismiss = {},
            onRetry = {}
        )
    }
}

@WearPreview
@Composable
private fun MainScreenLoadingPreview() {
    WearScreenPreview {
        MainScreenContent(
            state = MainState(query = PREVIEW_QUERY),
            repos = rememberPreviewPagingItems(
                items = emptyList(),
                loadStates = PreviewLoadStates.loading
            ),
            onSearchClick = {},
            onRepoClick = {},
            onRepoLongClick = {},
            onTopicsDismiss = {},
            onRetry = {}
        )
    }
}

@WearPreview
@Composable
private fun MainScreenEmptyPreview() {
    WearScreenPreview {
        MainScreenContent(
            state = MainState(query = PREVIEW_QUERY),
            repos = rememberPreviewPagingItems(
                items = emptyList(),
                loadStates = PreviewLoadStates.endOfList
            ),
            onSearchClick = {},
            onRepoClick = {},
            onRepoLongClick = {},
            onTopicsDismiss = {},
            onRetry = {}
        )
    }
}

@WearPreview
@Composable
private fun MainScreenRefreshErrorPreview() {
    WearScreenPreview {
        MainScreenContent(
            state = MainState(query = PREVIEW_QUERY),
            repos = rememberPreviewPagingItems(loadStates = PreviewLoadStates.refreshError),
            onSearchClick = {},
            onRepoClick = {},
            onRepoLongClick = {},
            onTopicsDismiss = {},
            onRetry = {}
        )
    }
}
