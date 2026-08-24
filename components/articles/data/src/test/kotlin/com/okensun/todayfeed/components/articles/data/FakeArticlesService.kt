package com.okensun.todayfeed.components.articles.data

import retrofit2.Response

/**
 * The source, replaced by a queue a test fills. It records what was asked for, which is how a
 * test says "and then it made no request at all" rather than only checking what came back.
 */
internal class FakeArticlesService : ArticlesService {
    private val queued = ArrayDeque<Response<ArticlesPage>>()

    val requests = mutableListOf<Request>()

    data class Request(
        val limit: Int,
        val offset: Int,
    )

    fun enqueue(page: ArticlesPage) {
        queued.addLast(Response.success(page))
    }

    override suspend fun articles(
        limit: Int,
        offset: Int,
    ): Response<ArticlesPage> {
        requests += Request(limit = limit, offset = offset)
        return queued.removeFirstOrNull()
            ?: error("The mediator asked for offset $offset and no page was queued for it.")
    }
}
