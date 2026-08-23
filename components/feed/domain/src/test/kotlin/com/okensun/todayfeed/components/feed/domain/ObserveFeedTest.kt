package com.okensun.todayfeed.components.feed.domain

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveFeedTest {
    @Test
    fun `puts the weather hero above the articles`() =
        runTest {
            val feed =
                ObserveFeed(
                    articles = FakeArticleRepository(listOf(article("1"), article("2"))),
                    weather = FakeWeatherRepository(weather())
                )

            feed().test {
                val items = awaitItem()

                assertTrue(items.first() is FeedItem.WeatherHero)
                assertEquals(2, items.count { it is FeedItem.ArticleRow })
            }
        }

    @Test
    fun `skips the hero when there is no weather yet, and still shows the articles`() =
        runTest {
            val feed =
                ObserveFeed(
                    articles = FakeArticleRepository(listOf(article("1"))),
                    weather = FakeWeatherRepository(weather = null)
                )

            feed().test {
                val items = awaitItem()

                assertTrue(items.none { it is FeedItem.WeatherHero })
                assertEquals(1, items.size)
            }
        }

    @Test
    fun `is empty only when both sources are empty`() =
        runTest {
            val feed =
                ObserveFeed(
                    articles = FakeArticleRepository(emptyList()),
                    weather = FakeWeatherRepository(weather = null)
                )

            feed().test {
                assertEquals(emptyList<FeedItem>(), awaitItem())
            }
        }

    @Test
    fun `emits again when one source changes`() =
        runTest {
            val articles = FakeArticleRepository(listOf(article("1")))
            val feed = ObserveFeed(articles = articles, weather = FakeWeatherRepository(weather()))

            feed().test {
                assertEquals(1, awaitItem().count { it is FeedItem.ArticleRow })

                articles.emit(listOf(article("1"), article("2"), article("3")))

                assertEquals(3, awaitItem().count { it is FeedItem.ArticleRow })
            }
        }
}
