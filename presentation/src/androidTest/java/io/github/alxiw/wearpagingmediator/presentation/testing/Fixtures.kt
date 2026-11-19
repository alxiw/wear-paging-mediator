package io.github.alxiw.wearpagingmediator.presentation.testing

import androidx.paging.PagingSource
import io.github.alxiw.wearpagingmediator.domain.model.Repo

/** How many results the [FakeRepository] serves in one page. */
internal const val PAGE_SIZE = 3

/**
 * The first result of a search: the star band of its 1M cap is the widest one, so no separator is
 * inserted above it (see `StarBand.separatorAbove`).
 */
internal val FIRST_REPO = repo(
    id = 1,
    name = "first-repo",
    description = "The first result of the search",
    language = "Kotlin",
    stars = 1_200_000,
    forks = 50_000
)

/** The second result: its band (500K and below) is narrower than the band of [FIRST_REPO]. */
internal val SECOND_REPO = repo(
    id = 2,
    name = "second-repo",
    description = "The second result of the search",
    language = "Java",
    stars = 400_000,
    forks = 1_200
)

/** The third result: the only one of its page in the band of 100 stars and below. */
internal val THIRD_REPO = repo(
    id = 3,
    name = "third-repo",
    description = "The third result of the search",
    language = null,
    stars = 90,
    forks = 12
)

/** The results of the first page of any query. */
internal val REPOS = listOf(FIRST_REPO, SECOND_REPO, THIRD_REPO)

/** The star bands the labels of [REPOS] separate: 500K and below, then 100 and below. */
internal val BAND_CAPS = listOf(500_000, 100)

internal fun repo(
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

/** One page of results, served by the source of the [FakeRepository]. */
internal fun page(
    items: List<Repo>,
    nextKey: Int? = null,
    prevKey: Int? = null
): PagingSource.LoadResult<Int, Repo> = PagingSource.LoadResult.Page(
    data = items,
    prevKey = prevKey,
    nextKey = nextKey
)
