package io.github.alxiw.wearpagingmediator.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alxiw.wearpagingmediator.domain.GetRecentQueriesUseCase
import io.github.alxiw.wearpagingmediator.domain.GetRepoTopicsUseCase
import io.github.alxiw.wearpagingmediator.domain.GetReposFlowUseCase
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY
import io.github.alxiw.wearpagingmediator.presentation.model.MainEffect
import io.github.alxiw.wearpagingmediator.presentation.model.MainIntent
import io.github.alxiw.wearpagingmediator.presentation.model.MainReducer
import io.github.alxiw.wearpagingmediator.presentation.model.MainState
import io.github.alxiw.wearpagingmediator.presentation.model.StarBand
import io.github.alxiw.wearpagingmediator.presentation.model.ListModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The state holder of the screen, written in MVI style: the UI sends [MainIntent]s, the reducer
 * turns them into a new [MainState] and one-off [MainEffect]s, and the results of the current
 * query are a separate stream because their load states are owned by the list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getReposFlow: GetReposFlowUseCase,
    private val getRepoTopics: GetRepoTopicsUseCase,
    private val getRecentQueries: GetRecentQueriesUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** The state of the screen, the only thing the UI renders. */
    private val _state = MutableStateFlow(restoredState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    /**
     * The actions the UI has to perform once. The channel buffers an effect until the screen
     * starts collecting it, so an intent that is handled before the screen subscribes is not lost.
     */
    private val effectChannel = Channel<MainEffect>(Channel.BUFFERED)
    val effects: Flow<MainEffect> = effectChannel.receiveAsFlow()

    /**
     * The flow of every visited query is kept, not the page window it produces: coming back to a
     * previous query (back resets the search to the default one) collects that flow again, so the
     * list starts from the cached head and the mediator refreshes the page around the top instead
     * of replaying a stale window that may begin in the middle of the search. Only
     * [MAX_CACHED_QUERIES] flows are kept, the least recently used one is dropped, and the window
     * of a single query is bounded by `maxSize` of the paging config.
     *
     * The database keeps more queries (`RecentQueriesCache`), so the results of a kept flow are
     * still there when it is collected again. The flow itself is reused, so `getReposFlow` is not
     * called for the same query twice while it is cached.
     */
    private val pagingDataCache =
        LinkedHashMap<SearchQuery, Flow<PagingData<ListModel>>>(MAX_CACHED_QUERIES, 0.75f, true)

    /** The load of the topics of the open dialog, cancelled when the dialog is closed. */
    private var topicsJob: Job? = null

    /** The load of the recent queries, cancelled when their dialog is closed. */
    private var recentQueriesJob: Job? = null

    val pagingDataFlow: Flow<PagingData<ListModel>> = state
        .map { it.query }
        .distinctUntilChanged()
        .flatMapLatest(::pagingDataFor)
        .cachedIn(viewModelScope)

    /** The only entry point of the screen: every event of the UI is turned into an intent. */
    fun onIntent(intent: MainIntent) {
        val previous = _state.value
        val reduction = MainReducer.reduce(previous, intent)
        _state.value = reduction.state
        persist(previous, reduction.state)
        reduction.effects.forEach { effectChannel.trySend(it) }

        when (intent) {
            is MainIntent.RepoClicked -> loadTopics(intent.repo.id)
            is MainIntent.TopicsDialogDismissed, is MainIntent.TopicClicked -> topicsJob?.cancel()
            is MainIntent.RecentQueriesRequested -> loadRecentQueries()
            is MainIntent.RecentQueriesDialogDismissed,
            is MainIntent.RecentQueryClicked -> recentQueriesJob?.cancel()
            else -> Unit
        }
    }

    /**
     * The queries are read every time the dialog is opened, so a query that was searched while the
     * dialog was closed shows up in it. A load of a closed dialog is dropped by the reducer.
     */
    private fun loadRecentQueries() {
        recentQueriesJob?.cancel()
        recentQueriesJob = viewModelScope.launch {
            val recent = try {
                getRecentQueries()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                onIntent(MainIntent.RecentQueriesLoadFailed)
                return@launch
            }
            onIntent(MainIntent.RecentQueriesLoaded(recent))
        }
    }

    /**
     * The topics are read only when the dialog is opened, they are not a part of the page. A load
     * of another repo or of a closed dialog is dropped by the reducer.
     */
    private fun loadTopics(repoId: Long) {
        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            val topics = try {
                getRepoTopics(repoId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                onIntent(MainIntent.TopicsLoadFailed(repoId))
                return@launch
            }
            onIntent(MainIntent.TopicsLoaded(repoId, topics))
        }
    }

    /**
     * The query survives process death: both the query of the results and the query the list was
     * scrolled to the top for are restored, so the results come back where they were left.
     */
    private fun restoredState(): MainState {
        val query = restoredQuery(KEY_LAST_SEARCH_QUERY)
        val lastQueryScrolled = restoredQuery(KEY_LAST_QUERY_SCROLLED)
        return MainState(
            query = query,
            lastQueryScrolled = lastQueryScrolled,
            // on start, the list position is restored, so there is nothing to scroll
            scrollToTopPending = false
        )
    }

    /** A value saved by an older version can be in another case: restore it canonical. */
    private fun restoredQuery(key: String): SearchQuery {
        val saved = savedStateHandle.get<String>(key) ?: DEFAULT_QUERY.value
        val query = SearchQuery.of(saved)
        return if (query.isEmpty) DEFAULT_QUERY else query
    }

    /** Saves only what changed, so the handle is not rewritten on every scroll. */
    private fun persist(previous: MainState, current: MainState) {
        if (current.query != previous.query) {
            savedStateHandle[KEY_LAST_SEARCH_QUERY] = current.query.value
        }
        if (current.lastQueryScrolled != previous.lastQueryScrolled) {
            savedStateHandle[KEY_LAST_QUERY_SCROLLED] = current.lastQueryScrolled.value
        }
    }

    /**
     * `pagingDataFlow` caches the currently collected PagingData in the ViewModel scope, while a
     * visited query is kept here as a cold flow: collecting it again starts a new Pager from the
     * cached head instead of replaying a window that may start mid-search.
     */
    private fun pagingDataFor(query: SearchQuery): Flow<PagingData<ListModel>> {
        pagingDataCache[query]?.let { return it }

        val data = searchRepo(query)
        pagingDataCache[query] = data

        // the cache is read in the order of access: the first entry is the least recently used one
        while (pagingDataCache.size > MAX_CACHED_QUERIES) {
            val eldest = pagingDataCache.entries.first()
            pagingDataCache.remove(eldest.key)
        }
        return data
    }

    private fun searchRepo(query: SearchQuery): Flow<PagingData<ListModel>> {
        return getReposFlow(query).map { pagingData ->
            pagingData
                .map { ListModel.RepoItem(it) }
                .insertSeparators { before, after ->
                    after ?: return@insertSeparators null
                    StarBand.separatorAbove(
                        before = before?.repo?.stars,
                        after = after.repo.stars
                    )?.let(ListModel::SeparatorItem)
                }
        }
    }

    companion object {
        private const val KEY_LAST_SEARCH_QUERY = "last_search_query"
        private const val KEY_LAST_QUERY_SCROLLED = "last_query_scrolled"

        /** How many visited queries keep their results ready to be shown again. */
        private const val MAX_CACHED_QUERIES = 3
    }
}
