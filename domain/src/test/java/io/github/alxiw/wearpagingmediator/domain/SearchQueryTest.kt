package io.github.alxiw.wearpagingmediator.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the canonical form of a query: GitHub search is case insensitive and splits the query
 * into words by whitespace, so the network request and the cache key have to agree on both,
 * otherwise `Android` and `android` would become two different searches.
 */
class SearchQueryTest {

    @Test
    fun caseIsIgnored() {
        assertEquals("android", SearchQuery.normalize("Android"))
        assertEquals("android", SearchQuery.normalize("ANDROID"))
    }

    @Test
    fun wordsAreSeparatedByASingleSpace() {
        assertEquals("android kotlin", SearchQuery.normalize("  Android \t kotlin \n"))
    }

    @Test
    fun quotesSurviveNormalization() {
        assertEquals("\"android kotlin\"", SearchQuery.normalize("\"Android  Kotlin\""))
    }

    @Test
    fun qualifiersAreKept() {
        assertEquals(
            "android language:kotlin stars:>100",
            SearchQuery.normalize("Android  language:Kotlin  stars:>100")
        )
    }

    @Test
    fun underscoreIsNotAWordSeparator() {
        assertEquals("android_kotlin", SearchQuery.normalize("android_kotlin"))
    }

    @Test
    fun blankQueryIsEmpty() {
        assertEquals("", SearchQuery.normalize("  \t\n "))
    }

    @Test
    fun lowerCaseDoesNotDependOnTheDefaultLocale() {
        // A lower case bound to the default locale would turn `I` into a dotless `ı` in Turkish.
        assertEquals("i", SearchQuery.normalize("I"))
    }
}
