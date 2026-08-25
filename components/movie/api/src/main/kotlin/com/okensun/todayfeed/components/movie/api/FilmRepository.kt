package com.okensun.todayfeed.components.movie.api

import com.okensun.todayfeed.components.movie.api.models.Film
import kotlinx.coroutines.flow.Flow

/**
 * The contract for reading the film catalogue. The same shape as the weather: collecting does
 * not fetch, so something has to say when.
 */
interface FilmRepository {
    /** What is held, best score first. Empty until something has been fetched. */
    fun observeFilms(): Flow<List<Film>>

    /** Asks again if the allowance says it is worth it. */
    suspend fun refresh()
}
