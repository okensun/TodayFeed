package com.okensun.todayfeed.components.articles.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.SavedStar
import com.okensun.todayfeed.core.designsystem.UnsavedStar

@Composable
fun ArticleRowCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSave: () -> Unit = {},
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // The text takes what is left, so a long title wraps rather than pushing the star
            // off the card.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            SaveControl(saved = article.saved, onToggle = onToggleSave)
        }
    }
}

/**
 * Filled means kept, hollow means not. The description is what a screen reader announces and
 * what a view test finds it by, so the two cannot drift apart.
 */
@Composable
internal fun SaveControl(
    saved: Boolean,
    onToggle: () -> Unit,
) = IconButton(onClick = onToggle) {
    Icon(
        imageVector = if (saved) SavedStar else UnsavedStar,
        contentDescription = if (saved) SAVED else SAVE
    )
}

internal const val SAVE = "Save"
internal const val SAVED = "Saved"
