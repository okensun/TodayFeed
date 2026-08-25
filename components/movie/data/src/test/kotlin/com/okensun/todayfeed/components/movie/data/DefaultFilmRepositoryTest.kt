package com.okensun.todayfeed.components.movie.data

import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.testing.FakeClock
import com.okensun.todayfeed.core.testing.FakeConnectivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class DefaultFilmRepositoryTest {
    private val service = FakeFilmService()
    private val clock = FakeClock()
    private val connectivity = FakeConnectivity()

    private val repository = DefaultFilmRepository(service, connectivity, clock)

    private fun film(
        id: String,
        title: String,
        score: String?,
    ) = FilmDto(id, title, "1992", "Hayao Miyazaki", "https://x/$id.jpg", score)

    @Test
    fun `collecting on its own asks for nothing`() =
        runTest {
            assertTrue(repository.observeFilms().first().isEmpty())

            assertEquals(0, service.calls)
        }

    @Test
    fun `a refresh brings the catalogue`() =
        runTest {
            repository.refresh()

            assertEquals(
                "Castle in the Sky",
                repository
                    .observeFilms()
                    .first()
                    .first()
                    .title
            )
            assertEquals(1, service.calls)
        }

    /** The score is on the card to explain the order, so the order has to be the score's. */
    @Test
    fun `the best score is first and a film with no score is last`() =
        runTest {
            service.answer =
                listOf(
                    film("1", "Porco Rosso", "94"),
                    film("2", "Unrated", null),
                    film("3", "Only Yesterday", "100")
                )

            repository.refresh()

            assertEquals(
                listOf("Only Yesterday", "Porco Rosso", "Unrated"),
                repository.observeFilms().first().map { it.title }
            )
        }

    /** Ties are common: several films share a score, and a row that reshuffles reads as a bug. */
    @Test
    fun `films with the same score are ordered by title`() =
        runTest {
            service.answer = listOf(film("1", "Whisper of the Heart", "91"), film("2", "Arrietty", "91"))

            repository.refresh()

            assertEquals(
                listOf("Arrietty", "Whisper of the Heart"),
                repository.observeFilms().first().map { it.title }
            )
        }

    /** Half a day is the allowance, so a second reading in the same session asks nothing. */
    @Test
    fun `a refresh inside the allowance asks for nothing`() =
        runTest {
            repository.refresh()

            clock.advanceBy(Duration.ofHours(11))
            repository.refresh()

            assertEquals(1, service.calls)
        }

    /** The point of not treating the catalogue as permanent: a correction does land. */
    @Test
    fun `past the allowance it asks again`() =
        runTest {
            repository.refresh()

            clock.advanceBy(Duration.ofHours(13))
            repository.refresh()

            assertEquals(2, service.calls)
        }

    @Test
    fun `with no connection it asks for nothing`() =
        runTest {
            connectivity.set(Connection.Offline)

            repository.refresh()

            assertEquals(0, service.calls)
            assertTrue(repository.observeFilms().first().isEmpty())
        }

    /**
     * A source that answers 200 with nothing has answered. Treating that as a failure left the
     * allowance unstarted, so every later ask went to the network again for as long as it stayed
     * empty.
     */
    @Test
    fun `an answer with nothing in it still starts the allowance`() =
        runTest {
            service.answer = emptyList()

            repository.refresh()
            repository.refresh()

            assertEquals(1, service.calls)
            assertTrue(repository.observeFilms().first().isEmpty())
        }

    @Test
    fun `a failure leaves what is held and does not buy silence`() =
        runTest {
            repository.refresh()
            service.failing = true

            clock.advanceBy(Duration.ofHours(13))
            repository.refresh()

            assertEquals(
                "Castle in the Sky",
                repository
                    .observeFilms()
                    .first()
                    .first()
                    .title
            )
            service.failing = false
            repository.refresh()
            assertEquals(3, service.calls)
        }
}
