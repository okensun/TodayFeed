package com.okensun.todayfeed.components.articles.data.network

import com.okensun.todayfeed.components.articles.data.toEntity
import com.okensun.todayfeed.core.network.TodayFeedJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * The fixture is a real response saved from the API, not written by hand, so a field the source
 * renames or a shape we guessed wrong shows up here rather than at run time.
 */
class ArticlesResponseTest {
    private val page =
        TodayFeedJson.decodeFromString<ArticlesPage>(
            checkNotNull(javaClass.classLoader?.getResourceAsStream("articles-page.json")) {
                "articles-page.json is missing from test resources"
            }.bufferedReader().readText()
        )

    @Test
    fun `a real response decodes, ignoring the fields we do not read`() {
        assertTrue("the source has plenty of articles", page.count > 1000)
        assertEquals(3, page.results.size)
        assertNotNull("a next page link is what tells us more exist", page.next)
    }

    @Test
    fun `every article carries what the card needs`() {
        page.results.forEach { dto ->
            assertTrue("title", dto.title.isNotBlank())
            assertTrue("source", dto.newsSite.isNotBlank())
            assertTrue("published date", dto.publishedAt.isNotBlank())
        }
    }

    @Test
    fun `mapping turns the numeric id into text and the date into an instant`() {
        val entity = page.results.first().toEntity()

        assertEquals(
            page.results
                .first()
                .id
                .toString(),
            entity.id
        )
        assertEquals(page.results.first().newsSite, entity.source)
        assertTrue("the date parsed", entity.publishedAt.isAfter(Instant.parse("2020-01-01T00:00:00Z")))
    }

    @Test
    fun `a date the source sends in a shape we do not expect does not lose the article`() {
        val broken = page.results.first().copy(publishedAt = "not a date")

        assertEquals(Instant.EPOCH, broken.toEntity().publishedAt)
        assertEquals(page.results.first().title, broken.toEntity().title)
    }

    @Test
    fun `a blank image url is treated as no picture`() {
        assertEquals(
            null,
            page.results
                .first()
                .copy(imageUrl = "   ")
                .toEntity()
                .imageUrl
        )
        assertEquals(
            null,
            page.results
                .first()
                .copy(imageUrl = null)
                .toEntity()
                .imageUrl
        )
    }
}
