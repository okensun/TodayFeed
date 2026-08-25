package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.movie.api.FilmRepository
import com.okensun.todayfeed.components.movie.api.models.Film
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeFilmRepository(
    films: List<Film> = emptyList(),
) : FilmRepository {
    private val current = MutableStateFlow(films)

    var refreshes = 0
        private set

    override fun observeFilms(): Flow<List<Film>> = current

    override suspend fun refresh() {
        refreshes++
    }

    fun set(films: List<Film>) {
        current.value = films
    }
}
