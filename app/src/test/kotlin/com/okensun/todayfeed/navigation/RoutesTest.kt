package com.okensun.todayfeed.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Routes are serialized when navigation puts them on the back stack, so an id that cannot
 * survive that round trip would fail only at run time. This catches it at build time.
 */
class RoutesTest {
    @Test
    fun `article id survives the route round trip`() {
        val route = ArticleDetailRoute(articleId = "39638")

        val restored = Json.decodeFromString<ArticleDetailRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
    }

    @Test
    fun `an id with characters that need escaping survives too`() {
        val route = ArticleDetailRoute(articleId = "a b/c?d&e")

        val restored = Json.decodeFromString<ArticleDetailRoute>(Json.encodeToString(route))

        assertEquals(route.articleId, restored.articleId)
    }
}
