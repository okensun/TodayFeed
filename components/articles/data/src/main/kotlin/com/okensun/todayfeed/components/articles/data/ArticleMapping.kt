package com.okensun.todayfeed.components.articles.data

import com.okensun.todayfeed.components.articles.api.models.Article
import com.okensun.todayfeed.components.articles.data.database.ArticleEntity
import com.okensun.todayfeed.components.articles.data.source.ArticleDto
import java.time.Instant

internal fun ArticleDto.toEntity(): ArticleEntity =
    ArticleEntity(
        id = id.toString(),
        title = title,
        summary = summary,
        source = newsSite,
        imageUrl = imageUrl?.takeIf { it.isNotBlank() },
        // The source sends an ISO instant. A row that cannot be dated would sort unpredictably, so
        // it is dropped to the epoch rather than dropped altogether.
        publishedAt = runCatching { Instant.parse(publishedAt) }.getOrDefault(Instant.EPOCH)
    )

internal fun ArticleEntity.toArticle(): Article =
    Article(
        id = id,
        title = title,
        summary = summary,
        source = source,
        imageUrl = imageUrl,
        publishedAt = publishedAt,
        saved = savedAt != null
    )
