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
            val sections = ObserveFeedSections(FakeWeatherRepository(weather()))

            sections().test {
                val list = awaitItem()

                assertEquals(1, list.size)
                assertTrue(list.first() is FeedSection.WeatherHero)
            }
        }

    @Test
    fun `a source with nothing is left out rather than shown empty`() =
        runTest {
            val sections = ObserveFeedSections(FakeWeatherRepository(weather = null))

            sections().test {
                assertEquals(emptyList<FeedSection>(), awaitItem())
            }
        }

    @Test
    fun `sections follow their source`() =
        runTest {
            val repository = FakeWeatherRepository(weather = null)
            val sections = ObserveFeedSections(repository)

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
