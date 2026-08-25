package com.okensun.todayfeed.components.feed.domain

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveFeedSectionsTest {
    @Test
    fun `a source with something to say becomes a section`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather()), FakeFilmRepository())

            sections().test {
                val list = awaitItem()

                assertEquals(1, list.size)
                assertTrue(list.first() is FeedSection.WeatherHero)
            }
        }

    @Test
    fun `a source with nothing is left out rather than shown empty`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather = null), FakeFilmRepository())

            sections().test {
                assertEquals(emptyList<FeedSection>(), awaitItem())
            }
        }

    /** Task 3.1: two sources, and the order they appear in is the order they are built. */
    @Test
    fun `both sources present gives the weather first and the films second`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather()), FakeFilmRepository(films()))

            sections().test {
                val list = awaitItem()

                assertEquals(2, list.size)
                assertTrue(list[0] is FeedSection.WeatherHero)
                assertTrue(list[1] is FeedSection.Films)
            }
        }

    @Test
    fun `films alone still make a section`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather = null), FakeFilmRepository(films()))

            sections().test {
                val list = awaitItem()

                assertEquals(1, list.size)
                assertTrue(list.first() is FeedSection.Films)
            }
        }

    /** One source failing must not cost the other. An empty catalogue is a missing section. */
    @Test
    fun `no films leaves the weather alone`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather()), FakeFilmRepository())

            sections().test {
                val list = awaitItem()

                assertEquals(1, list.size)
                assertTrue(list.first() is FeedSection.WeatherHero)
            }
        }

    private fun films() =
        listOf(
            com.okensun.todayfeed.components.movie.api.models.Film(
                id = "1",
                title = "Castle in the Sky",
                year = "1986",
                director = "Hayao Miyazaki",
                bannerUrl = null
            )
        )

    @Test
    fun `sections follow their source`() =
        runTest {
            val repository = FakeWeatherRepository(weather = null)
            val sections = ObserveFeedSections(repository, FakeFilmRepository())

            sections().test {
                assertEquals(emptyList<FeedSection>(), awaitItem())

                repository.emit(weather("Kaohsiung"))
                val list = awaitItem()

                assertEquals(
                    "Kaohsiung",
                    (list.single() as FeedSection.WeatherHero).weather.placeName
                )
            }
        }
}
