package io.github.alxiw.wearpagingmediator.presentation.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.StarBand
import io.github.alxiw.wearpagingmediator.presentation.model.ListModel
import io.github.alxiw.wearpagingmediator.presentation.util.CountFormatter
import io.github.alxiw.wearpagingmediator.presentation.util.UiText
import kotlinx.coroutines.flow.flowOf

/** The results of the previews: their star bands are the ones the separators of a search show. */
internal val previewRepos: List<Repo> = listOf(
    previewRepo(
        id = 1,
        name = "compose",
        description = "A declarative UI toolkit for Kotlin",
        stars = 1_200_000,
        forks = 50_000,
        language = "Kotlin"
    ),
    previewRepo(
        id = 2,
        name = "paging",
        description = "Load and show pages of data from a large data set",
        stars = 400_000,
        forks = 1_200,
        language = "Kotlin"
    ),
    previewRepo(
        id = 3,
        name = "wear",
        description = "A UI library for the watch",
        stars = 90,
        forks = 12,
        language = null
    )
)

/** The results with the separators between their star bands, the way the ViewModel maps them. */
internal val previewModels: List<ListModel> = buildList {
    previewRepos.forEachIndexed { index, repo ->
        val cap = StarBand.separatorAbove(
            before = previewRepos.getOrNull(index - 1)?.stars,
            after = repo.stars
        )
        if (cap != null) add(ListModel.SeparatorItem(starsLabel(cap)))
        add(ListModel.RepoItem(repo))
    }
}

/** The load states of the previews, one for every state the list can report. */
internal object PreviewLoadStates {
    val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = false),
        append = LoadState.NotLoading(endOfPaginationReached = false)
    )
    val loading = LoadStates(
        refresh = LoadState.Loading,
        prepend = LoadState.NotLoading(endOfPaginationReached = false),
        append = LoadState.NotLoading(endOfPaginationReached = false)
    )
    val refreshError = loaded.copy(refresh = FAILURE)
    val appendError = loaded.copy(append = FAILURE)
    val endOfList = loaded.copy(append = LoadState.NotLoading(endOfPaginationReached = true))
}

/**
 * The paging items of a preview: the static data is reported with the given [loadStates], so a
 * preview can show the same loading and error states the network reports on a real watch.
 */
@Composable
internal fun rememberPreviewPagingItems(
    items: List<ListModel> = previewModels,
    loadStates: LoadStates = PreviewLoadStates.loaded
): LazyPagingItems<ListModel> {
    val pagingData = remember(items, loadStates) {
        PagingData.from(items, sourceLoadStates = loadStates)
    }
    return flowOf(pagingData).collectAsLazyPagingItems()
}

internal fun previewRepo(
    id: Long,
    name: String,
    description: String? = "Description of $name",
    language: String? = "Kotlin",
    stars: Int = 1_000,
    forks: Int = 100,
    url: String = "https://github.com/alxiw/$name"
) = Repo(
    id = id,
    name = name,
    description = description,
    url = url,
    stars = stars,
    forks = forks,
    language = language
)

private val FAILURE = LoadState.Error(RuntimeException("Preview failure"))

private fun starsLabel(cap: Int): UiText = when {
    cap > 0 -> UiText.StringResource(R.string.stars_label, CountFormatter.format(cap))
    else -> UiText.StringResource(R.string.stars_zero)
}
