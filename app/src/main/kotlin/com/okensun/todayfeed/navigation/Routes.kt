package com.okensun.todayfeed.navigation

import kotlinx.serialization.Serializable

/**
 * Routes are types, not strings. `ArticleDetail(id)` is checked when the project builds,
 * while "detail/{id}" is only checked when the app runs and a typo in it crashes.
 *
 * The graph lives here in :app because no component may know where another component's
 * screens are. Components hand out callbacks instead. See DECISIONS.md.
 */
@Serializable
data object ReadingRoute

@Serializable
data object SavedRoute

@Serializable
data class ArticleDetailRoute(val articleId: String)
