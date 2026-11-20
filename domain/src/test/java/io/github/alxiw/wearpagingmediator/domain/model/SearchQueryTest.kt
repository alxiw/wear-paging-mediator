package io.github.alxiw.wearpagingmediator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the canonical form of a query: GitHub search is case insensitive and splits the query
 * into words by whitespace, so the network request and the cache key have to agree on both,
 * otherwise `Android` and `android` would become two different searches.
 */
class SearchQueryTest {

    @Test
    fun caseIsIgnored() {
        assertEquals("android", SearchQuery.of("Android").value)
        assertEquals("android", SearchQuery.of("ANDROID").value)
    }

    @Test
    fun wordsAreSeparatedByASingleSpace() {
        assertEquals("android kotlin", SearchQuery.of("  Android \t kotlin \n").value)
    }

    @Test
    fun quotedWordsStayOnePhrase() {
        assertEquals("\"android kotlin\"", SearchQuery.of("\"Android  Kotlin\"").value)
    }

    @Test
    fun qualifiersAreKept() {
        assertEquals(
            "android language:kotlin stars:>100",
            SearchQuery.of("Android  language:Kotlin  stars:>100").value
        )
    }

    @Test
    fun underscoreIsNotAWordSeparator() {
        assertEquals("android_kotlin", SearchQuery.of("android_kotlin").value)
    }

    @Test
    fun blankQueryIsEmpty() {
        assertTrue(SearchQuery.of("  \t\n ").isEmpty)
    }

    @Test
    fun lowerCaseDoesNotDependOnTheDefaultLocale() {
        // A lower case bound to the default locale would turn `I` into a dotless `ı` in Turkish.
        assertEquals("i", SearchQuery.of("I").value)
    }

    @Test
    fun queriesWithTheSameCanonicalFormAreTheSameKey() {
        // The paging cache and the database are keyed by the value itself: one search, one entry.
        assertEquals(SearchQuery.of("  Android \t Kotlin "), SearchQuery.of("android kotlin"))
        assertEquals(
            SearchQuery.of("android kotlin").hashCode(),
            SearchQuery.of("Android\tKotlin").hashCode()
        )
    }
}
