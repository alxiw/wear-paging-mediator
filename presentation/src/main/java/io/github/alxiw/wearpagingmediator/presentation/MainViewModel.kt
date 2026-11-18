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
import io.github.alxiw.wearpagingmediator.presentation.model.UiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getReposFlow: GetReposFlowUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val actionStateFlow = MutableSharedFlow<UiAction>()

    // filters search events, deduplicates values, and emits an initial empty query on start
    private val searches = savedStateHandle
        .getStateFlow(KEY_QUERY, DEFAULT_QUERY)
        .map { UiAction.Search(query = it) }
        .distinctUntilChanged()

    // filters only Scroll events
    private val queriesScrolled = actionStateFlow
        .filterIsInstance<UiAction.Scroll>()
        .distinctUntilChanged()
        // shareIn with replay = 1 makes the flow "hot" while caching the last query scrolled,
        // otherwise each flatMapLatest invocation would lose the last query scrolled,
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            replay = 1
        )
        .onStart {
            emit(
                UiAction.Scroll(
                    currentQuery = savedStateHandle[KEY_QUERY] ?: DEFAULT_QUERY
                )
            )
        }

    // cancels the previous search query and triggers a new repository request on each new query
    val pagingDataFlow: Flow<PagingData<UiModel>> = searches
        .flatMapLatest { searchRepo(it.query) }
        // caches pages in memory so data isn't re-fetched on configuration changes
        .cachedIn(viewModelScope)

    private fun searchRepo(queryString: String): Flow<PagingData<UiModel>> {
        return getReposFlow(queryString)
            .map { pagingData -> pagingData.map { UiModel.RepoItem(it) } }
            .map {
                it.insertSeparators { before, after ->
                    if (after == null) {
                        // we're at the end of the list
                        return@insertSeparators null
                    }

                    val afterStarCount = after.repo.stars

                    if (before == null) {
                        // we're at the beginning of the list
                        return@insertSeparators if (afterStarCount > 10_000) {
                            UiModel.SeparatorItem("${afterStarCount / 10_000}0K+ stars")
                        } else {
                            null
                        }
                    }

                    val beforeRoundedStarCount = before.repo.stars

                    // check between 2 items
                    if (beforeRoundedStarCount / 10_000 > afterStarCount / 10_000) {
                        if (afterStarCount / 10_000 >= 1) {
                            UiModel.SeparatorItem("${afterStarCount / 10_000}0K+ stars")
                        } else {
                            UiModel.SeparatorItem("< 10K+ stars")
                        }
                    } else {
                        null
                    }
                }
            }
    }

    // combines both flows to compute hasNotScrolledForCurrentSearch — a flag indicating
    // whether the user hasn't scrolled the list since the latest search;
    // used by the UI to scroll the list back to the top on a new query
    val state: StateFlow<UiState> = combine(searches, queriesScrolled, ::Pair)
        .map { (search, scroll) ->
            UiState(
                query = search.query,
                lastQueryScrolled = scroll.currentQuery,
                // If the search query matches the scroll query, the user has scrolled
                hasNotScrolledForCurrentSearch = search.query != scroll.currentQuery
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = UiState(query = savedStateHandle[KEY_QUERY] ?: DEFAULT_QUERY)
        )

    // single entry point: all events are routed into a single MutableSharedFlow
    fun accept(action: UiAction) {
        when (action) {
            is UiAction.Search -> savedStateHandle[KEY_QUERY] = action.query
            is UiAction.Scroll -> viewModelScope.launch { actionStateFlow.emit(action) }
        }
    }
}

sealed class UiAction {
    data class Search(val query: String) : UiAction()
    data class Scroll(val currentQuery: String) : UiAction()
}

data class UiState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    val hasNotScrolledForCurrentSearch: Boolean = false
)

private const val KEY_QUERY = "query"
const val DEFAULT_QUERY = "Android"
