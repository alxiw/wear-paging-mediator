package io.github.alxiw.wearpagingmediator.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alxiw.wearpagingmediator.domain.GetReposFlowUseCase
import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY
import io.github.alxiw.wearpagingmediator.presentation.model.StarBand
import io.github.alxiw.wearpagingmediator.presentation.model.UiAction
import io.github.alxiw.wearpagingmediator.presentation.model.UiModel
import io.github.alxiw.wearpagingmediator.presentation.model.UiState
import io.github.alxiw.wearpagingmediator.presentation.util.CountFormatter
import io.github.alxiw.wearpagingmediator.presentation.util.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getReposFlow: GetReposFlowUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val searches = savedStateHandle
        .getStateFlow(KEY_LAST_SEARCH_QUERY, DEFAULT_QUERY)
        .map { UiAction.Search(query = it) }
        .distinctUntilChanged()

    private val scrollsToTop = savedStateHandle
        .getStateFlow(KEY_LAST_QUERY_SCROLLED, DEFAULT_QUERY)
        .map { UiAction.Scroll(query = it) }
        .distinctUntilChanged()

    /**
     * The results of every visited query are kept: coming back to a previous query (back resets the
     * search to the default one) shows them from the same window instead of starting a new Pager
     * and a new refresh. Only [MAX_CACHED_QUERIES] queries are kept, the least recently used one is
     * dropped together with its window, while the window of a single query is bounded by
     * `maxSize` of the paging config.
     */
    private val pagingDataCache =
        LinkedHashMap<String, CachedQuery>(MAX_CACHED_QUERIES, 0.75f, true)

    val pagingDataFlow: Flow<PagingData<UiModel>> = searches
        .flatMapLatest { search -> pagingDataFor(search.query) }
        .cachedIn(viewModelScope)

    // combines two flows and computes scrollToTopPending flag that tells the UI to scroll
    // the list back to the top: the query has changed and the list has not been scrolled
    // to the top for it yet
    val state: StateFlow<UiState> = combine(searches, scrollsToTop, ::Pair)
        .map { (search, scroll) ->
            UiState(
                query = search.query,
                lastQueryScrolled = scroll.query,
                // if the search query matches the scroll query, the top is already shown
                scrollToTopPending = search.query != scroll.query
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = UiState(
                query = savedStateHandle[KEY_LAST_SEARCH_QUERY] ?: DEFAULT_QUERY,
                lastQueryScrolled = savedStateHandle[KEY_LAST_QUERY_SCROLLED] ?: DEFAULT_QUERY,
                // on start, the list position is restored, so there is nothing to scroll
                scrollToTopPending = false
            )
        )

    fun accept(action: UiAction) {
        when (action) {
            is UiAction.Search -> savedStateHandle[KEY_LAST_SEARCH_QUERY] = action.query
            is UiAction.Scroll -> savedStateHandle[KEY_LAST_QUERY_SCROLLED] = action.query
        }
    }

    /**
     * `cachedIn` never stops collecting the Pager on its own, so every query gets a scope of its
     * own: its window stays ready while the query is cached and is released as soon as the cache
     * drops the query.
     */
    private fun pagingDataFor(queryString: String): Flow<PagingData<UiModel>> {
        pagingDataCache[queryString]?.let { return it.data }

        // a failure of one query must not cancel the scope of the ViewModel
        val job = SupervisorJob(viewModelScope.coroutineContext[Job])
        val data = searchRepo(queryString)
            .cachedIn(CoroutineScope(viewModelScope.coroutineContext + job))
        pagingDataCache[queryString] = CachedQuery(job = job, data = data)

        // the cache is read in the order of access: the first entry is the least recently used one
        while (pagingDataCache.size > MAX_CACHED_QUERIES) {
            val eldest = pagingDataCache.entries.first()
            pagingDataCache.remove(eldest.key)
            eldest.value.job.cancel()
        }
        return data
    }

    private fun searchRepo(queryString: String): Flow<PagingData<UiModel>> {
        return getReposFlow(queryString).map { pagingData ->
            pagingData
                .map { UiModel.RepoItem(it) }
                .insertSeparators { before, after ->
                    after ?: return@insertSeparators null
                    val band = StarBand.separatorAbove(
                        before = before?.let { it.repo.stars },
                        after = after.repo.stars
                    ) ?: return@insertSeparators null
                    UiModel.SeparatorItem(starsLabel(band))
                }
        }
    }

    private fun starsLabel(cap: Int): UiText = when {
        cap > 0 -> UiText.StringResource(R.string.stars_label, CountFormatter.format(cap))
        else -> UiText.StringResource(R.string.stars_zero)
    }

    /** The results of one visited query, kept until the cache drops the query. */
    private class CachedQuery(
        val job: Job,
        val data: Flow<PagingData<UiModel>>
    )

    companion object {
        private const val KEY_LAST_SEARCH_QUERY = "last_search_query"
        private const val KEY_LAST_QUERY_SCROLLED = "last_query_scrolled"

        /** How many visited queries keep their results ready to be shown again. */
        private const val MAX_CACHED_QUERIES = 3
    }
}
