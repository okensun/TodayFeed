package com.okensun.todayfeed.components.movie.data

import com.okensun.todayfeed.core.network.TodayFeedJson
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The fixture is a real saved answer, trimmed to two films, so a field that moves is caught. */
class FilmMappingTest {
    private val json: Json = TodayFeedJson

    @Test
    fun `a real answer decodes into what the row needs`() {
        val films = json.decodeFromString<List<FilmDto>>(TWO_FILMS).map { it.toFilm() }

        assertEquals(2, films.size)
        assertEquals("Castle in the Sky", films[0].title)
        assertEquals("1986", films[0].year)
        assertEquals("Hayao Miyazaki", films[0].director)
        assertEquals(true, films[0].bannerUrl?.startsWith("https://") == true)
        assertEquals(95, films[0].score)
    }

    /** A score the row cannot order by is no score, not a zero that would sort it last-but-one. */
    @Test
    fun `a score that is not a number is no score`() {
        assertNull(FilmDto("1", "T", "1990", "D", rtScore = "N/A").toFilm().score)
        assertNull(FilmDto("1", "T", "1990", "D").toFilm().score)
    }

    /** The source has sent an empty string for a banner before, and that is not a picture. */
    @Test
    fun `a blank banner counts as no picture`() {
        val film = FilmDto("1", "T", "1990", "D", movieBanner = "  ").toFilm()

        assertNull(film.bannerUrl)
    }

    @Test
    fun `a missing banner is not a decoding failure`() {
        val film = json.decodeFromString<FilmDto>("""{"id":"1","title":"T","release_date":"1990","director":"D"}""")

        assertNull(film.toFilm().bannerUrl)
    }

    private companion object {
        val TWO_FILMS =
            java.io
                .File("src/test/resources/two-films.json")
                .takeIf { it.exists() }
                ?.readText()
                ?: error("The saved answer is missing")
    }
}
