package com.okensun.todayfeed.components.movie.data

import retrofit2.Response
import java.io.IOException

internal class FakeFilmService : FilmService {
    var calls = 0
        private set

    /** Set to make the next call fail the way a real one can. */
    var failing = false

    /** Set to answer with something other than the one film most tests only need to count. */
    var answer =
        listOf(FilmDto("1", "Castle in the Sky", "1986", "Hayao Miyazaki", "https://x/1.jpg", "95"))

    override suspend fun films(): Response<List<FilmDto>> {
        calls++
        if (failing) throw IOException("no network")
        return Response.success(answer)
    }
}
