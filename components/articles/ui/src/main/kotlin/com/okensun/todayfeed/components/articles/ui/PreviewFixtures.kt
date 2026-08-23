package com.okensun.todayfeed.components.articles.ui

import com.okensun.todayfeed.components.articles.api.Article
import java.time.Instant

/** Shared by the previews in this package so the same article is not written three times. */
internal fun previewArticle(id: String = "1") =
    Article(
        id = id,
        title = "CNES seeks partners to mass produce compact optical telescopes",
        summary =
            "The French space agency is looking for an industrial partner to develop and " +
                "qualify a compact optical telescope for future satellite constellations.",
        source = "European Spaceflight",
        imageUrl = null,
        publishedAt = Instant.EPOCH
    )
