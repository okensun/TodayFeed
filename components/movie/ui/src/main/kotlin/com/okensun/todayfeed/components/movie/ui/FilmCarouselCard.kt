package com.okensun.todayfeed.components.movie.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.okensun.todayfeed.components.movie.api.models.Film
import com.okensun.todayfeed.core.designsystem.ArticleImage
import com.okensun.todayfeed.core.designsystem.SectionTitle
import com.okensun.todayfeed.core.designsystem.ThemePreviews
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme

/**
 * A row that scrolls sideways inside a feed that scrolls down. It keeps its own state, so
 * scrolling the feed does not take the reader back to the first film.
 */
@Composable
fun FilmCarouselCard(
    films: List<Film>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    if (films.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(TITLE)
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(films)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(films: List<Film>) =
    items(count = films.size, key = { films[it].id }) { index ->
        FilmCard(films[index])
    }

@Composable
private fun FilmCard(film: Film) =
    Card(modifier = Modifier.padding(end = 12.dp).width(220.dp)) {
        ArticleImage(
            url = film.bannerUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .clip(MaterialTheme.shapes.medium)
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = film.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = film.line(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

/** The row is ordered by the score, so the score has to be on the card to explain the order. */
private fun Film.line() = score?.let { "$year  ·  $it%" } ?: year

private const val TITLE = "Films"

@ThemePreviews
@Composable
private fun FilmCarouselPreview() =
    TodayFeedTheme {
        FilmCarouselCard(
            films =
                listOf(
                    Film("1", "Castle in the Sky", "1986", "Hayao Miyazaki", null, 95),
                    Film("2", "Grave of the Fireflies", "1988", "Isao Takahata", null, 97)
                )
        )
    }
