package io.github.alxiw.wearpagingmediator.presentation.ui

import android.app.Activity
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY
import io.github.alxiw.wearpagingmediator.presentation.MainViewModel
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.ListFooter
import io.github.alxiw.wearpagingmediator.presentation.model.ListNotice
import io.github.alxiw.wearpagingmediator.presentation.model.ListStatus
import io.github.alxiw.wearpagingmediator.presentation.model.UiAction
import io.github.alxiw.wearpagingmediator.presentation.model.UiModel
import kotlin.text.ifEmpty

/**
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

    val repos: LazyPagingItems<UiModel> = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    val uiState by viewModel.state.collectAsState()
    val currentQuery = uiState.query

    // Back resets a custom search back to the default query. On the default query there is
    // nothing to reset, so leave the event to the system and let it finish the activity.
    BackHandler(enabled = currentQuery != DEFAULT_QUERY) {
        viewModel.accept(UiAction.Search(DEFAULT_QUERY))
    }

    // A new query (including the reset done by back) always starts the results from the top.
    // The query is read before the suspending scroll, so the scroll is reported for the query
    // it was actually made for.
    LaunchedEffect(uiState.query, uiState.scrollToTopPending) {
        if (uiState.scrollToTopPending) {
            val query = uiState.query
            listState.scrollToItem(0)
            viewModel.accept(UiAction.Scroll(query))
        }
    }

    // The same query is not a new search: the paging flow ignores it, so only the network part is
    // repeated and the cached results stay on screen while they are refreshed.
    val launcher = rememberLauncherForActivityResult(WearSearchContract()) { query ->
        Log.d("HELLO", "QUERY: $query")
        val newQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        when (newQuery) {
            null -> Unit
            currentQuery -> repos.refresh()
            else -> viewModel.accept(UiAction.Search(newQuery))
        }
    }

    // What is reported instead of the results (there are none yet) and right after the last item.
    val loadState = repos.loadState
    val notice = ListStatus.notice(loadState.refresh, loadState.append, repos.itemCount)
    val footer = ListStatus.footer(loadState.refresh, loadState.append, repos.itemCount)

    val searchLabel = stringResource(R.string.search_label)

    AppScaffold {
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
                        query = uiState.query.ifEmpty { DEFAULT_QUERY },
                        onClick = { launcher.launch(searchLabel) }
                    )
                }

                // the query has nothing to show yet: the head of the list is never taken by a
                // state of the load, a refresh of the results that are already shown is not
                // reported here at all
                when (notice) {
                    ListNotice.Loading -> item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    ListNotice.Empty -> item {
                        MessageCard(text = stringResource(R.string.empty))
                    }
                    null -> Unit
                }

                // repos items
                items(
                    count = repos.itemCount,
                    key = repos.itemKey {
                        when (it) {
                            is UiModel.RepoItem -> it.repo.id
                            is UiModel.SeparatorItem -> it.description.stableKey
                        }
                    },
                    contentType = repos.itemContentType {
                        when (it) {
                            is UiModel.RepoItem -> "repo"
                            is UiModel.SeparatorItem -> "separator"
                        }
                    }
                ) { index ->
                    when (val item = repos[index]) {
                        is UiModel.RepoItem -> RepoCard(
                            repo = item.repo,
                            onClick = {
                                Log.d("HELLO", "Repo ${item.repo.name} clicked")
                                val intent = Intent(Intent.ACTION_VIEW, item.repo.url.toUri())
                                context.startActivity(intent)
                            }
                        )
                        is UiModel.SeparatorItem -> SeparatorCard(description = item.description)
                        null -> Unit // Placeholder for loading
                    }
                }

                // load state below the loaded results, a failed refresh included
                when (footer) {
                    ListFooter.RefreshError -> item {
                        ErrorCard(
                            message = stringResource(R.string.error_refresh),
                            onRetry = repos::retry
                        )
                    }
                    ListFooter.Loading -> item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    ListFooter.AppendError -> item {
                        ErrorCard(
                            message = stringResource(R.string.error_append),
                            onRetry = repos::retry
                        )
                    }
                    ListFooter.EndOfList -> item {
                        MessageCard(text = stringResource(R.string.end_of_list))
                    }
                    null -> Unit
                }
            }
        }
    }
}

class WearSearchContract : ActivityResultContract<String, String?>() {
    override fun createIntent(context: Context, input: String): Intent {
        val remoteInput = RemoteInput.Builder(EXTRA_VOICE_REPLY)
            .setLabel(input)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_SEARCH)
            }
            .build()
        return RemoteInputIntentHelper.createActionRemoteInputIntent().also {
            RemoteInputIntentHelper.putRemoteInputsExtra(it, listOf(remoteInput))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return RemoteInput.getResultsFromIntent(intent)?.getString(EXTRA_VOICE_REPLY)
    }
}

private const val EXTRA_VOICE_REPLY = "extra_voice_reply"
