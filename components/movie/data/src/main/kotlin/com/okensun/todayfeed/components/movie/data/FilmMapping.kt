package com.okensun.todayfeed.components.movie.data

import com.okensun.todayfeed.components.movie.api.models.Film

internal fun FilmDto.toFilm() =
    Film(
        id = id,
        title = title,
        year = releaseDate,
        director = director,
        bannerUrl = movieBanner?.takeIf { it.isNotBlank() },
        // The score arrives as text. Anything that is not a number is no score, and no score
        // sorts last rather than as a zero.
        score = rtScore?.toIntOrNull()
    )
