package com.okensun.todayfeed.components.feed.domain

import app.cash.turbine.test
import com.okensun.todayfeed.components.movie.api.models.Film
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveFeedSectionsTest {
    /** Task 3.1: two sources, and the order they appear in is the order they are built. */
    @Test
    fun `the weather comes first and the films second`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather()), FakeFilmRepository(films()))

            sections().test {
                val list = awaitItem()

                assertEquals(2, list.size)
                assertTrue(list[0] is FeedSection.WeatherHero)
                assertTrue(list[1] is FeedSection.Films)
            }
        }

    /**
     * A source with nothing keeps its place. Left out, it made the list grow under the reader, and
     * a block appearing above what someone is reading either moves them or goes unseen.
     */
    @Test
    fun `a source with nothing still has its section, holding nothing`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather = null), FakeFilmRepository())

            sections().test {
                val list = awaitItem()

                assertEquals(2, list.size)
                assertFalse("nothing has answered", list.any { it.hasContent })
            }
        }

    /** One source failing must not cost the other its block or its content. */
    @Test
    fun `one source answering leaves the other section in place`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather()), FakeFilmRepository())

            sections().test {
                val list = awaitItem()

                assertTrue("the weather answered", list[0].hasContent)
                assertFalse("the films did not", list[1].hasContent)
            }
        }

    /** How many blocks there are never changes. Only what is inside one does. */
    @Test
    fun `a source answering changes what is in its section, not how many there are`() =
        runTest {
            val repository = FakeWeatherRepository(weather = null)
            val sections = ObserveFeedSections(repository, FakeFilmRepository())

            sections().test {
                assertEquals(2, awaitItem().size)

                repository.emit(weather("Kaohsiung"))
                val list = awaitItem()

                assertEquals(2, list.size)
                assertEquals("Kaohsiung", (list[0] as FeedSection.WeatherHero).weather?.placeName)
            }
        }

    private fun films() =
        listOf(
            Film(
                id = "1",
                title = "Castle in the Sky",
                year = "1986",
                director = "Hayao Miyazaki",
                bannerUrl = null
            )
        )
}
