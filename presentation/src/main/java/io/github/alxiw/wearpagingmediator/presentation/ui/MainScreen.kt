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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import io.github.alxiw.wearpagingmediator.presentation.DEFAULT_QUERY
import io.github.alxiw.wearpagingmediator.presentation.MainViewModel
import io.github.alxiw.wearpagingmediator.presentation.UiAction
import io.github.alxiw.wearpagingmediator.presentation.model.UiModel
import kotlin.text.ifEmpty

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val listState = rememberTransformingLazyColumnState()
    val context = LocalContext.current

    val repos: LazyPagingItems<UiModel> = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    val uiState by viewModel.state.collectAsState()
    val currentQuery = uiState.query

    BackHandler(enabled = currentQuery.isNotEmpty()) {
        viewModel.accept(UiAction.Search(DEFAULT_QUERY))
    }

    val launcher = rememberLauncherForActivityResult(WearSearchContract()) { query ->
        Log.d("HELLO", "QUERY: $query")
        query?.trim()?.takeIf { it.isNotEmpty() }?.let { viewModel.accept(UiAction.Search(it)) }
    }

    AppScaffold {
        ScreenScaffold(
            scrollState = listState,
        ) { contentPadding ->
            if (repos.loadState.refresh is LoadState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@ScreenScaffold
            }

            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                // search button
                item {
                    Button(
                        onClick = { launcher.launch("Search repos") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.query.ifEmpty { DEFAULT_QUERY },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // repos items
                items(
                    count = repos.itemCount,
                    key = repos.itemKey {
                        when (it) {
                            is UiModel.RepoItem -> it.repo.id
                            is UiModel.SeparatorItem -> it.description
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

                // loading item
                if (repos.loadState.append is LoadState.Loading) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
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
