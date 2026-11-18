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
import io.github.alxiw.wearpagingmediator.presentation.util.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val queriesScrolled = savedStateHandle
        .getStateFlow(KEY_LAST_QUERY_SCROLLED, DEFAULT_QUERY)
        .map { UiAction.Scroll(query = it) }
        .distinctUntilChanged()

    val pagingDataFlow: Flow<PagingData<UiModel>> = searches
        .flatMapLatest { searchRepo(it.query) }
        .cachedIn(viewModelScope)

    // combines two flows and computes hasNotScrolledForCurrentSearch flag
    // that indicates whether the user hasn't scrolled the list since the latest search;
    // used by the UI to scroll the list back to the top on a new query
    val state: StateFlow<UiState> = combine(searches, queriesScrolled, ::Pair)
        .map { (search, scroll) ->
            UiState(
                query = search.query,
                lastQueryScrolled = scroll.query,
                // if the search query matches the scroll query, the user has scrolled
                hasNotScrolledForCurrentSearch = search.query != scroll.query
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = UiState(
                query = savedStateHandle[KEY_LAST_SEARCH_QUERY] ?: DEFAULT_QUERY,
                lastQueryScrolled = savedStateHandle[KEY_LAST_QUERY_SCROLLED] ?: DEFAULT_QUERY,
                // on start, we usually don't want to force scroll
                hasNotScrolledForCurrentSearch = false
            )
        )

    fun accept(action: UiAction) {
        when (action) {
            is UiAction.Search -> savedStateHandle[KEY_LAST_SEARCH_QUERY] = action.query
            is UiAction.Scroll -> savedStateHandle[KEY_LAST_QUERY_SCROLLED] = action.query
        }
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
        cap >= M -> UiText.StringResource(R.string.stars_m, cap / M)
        cap >= K -> UiText.StringResource(R.string.stars_k, cap / K)
        cap > 0 -> UiText.StringResource(R.string.stars_n, cap)
        else -> UiText.StringResource(R.string.stars_zero)
    }

    companion object {
        private const val KEY_LAST_SEARCH_QUERY = "last_search_query"
        private const val KEY_LAST_QUERY_SCROLLED = "last_query_scrolled"
        private const val K = 1_000
        private const val M = 1_000_000
    }
}
