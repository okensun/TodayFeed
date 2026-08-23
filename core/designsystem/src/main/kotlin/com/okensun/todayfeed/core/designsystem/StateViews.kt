package com.okensun.todayfeed.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun CentredMessage(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (action != null) {
            Column(modifier = Modifier.padding(top = 16.dp)) { action() }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Loading",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
fun EmptyState(title: String, body: String? = null, modifier: Modifier = Modifier) {
    CentredMessage(title = title, body = body, modifier = modifier)
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CentredMessage(
        title = "Something went wrong",
        body = message,
        modifier = modifier,
        action = { Button(onClick = onRetry) { Text("Try again") } },
    )
}

@Composable
fun OfflineState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CentredMessage(
        title = "You are offline",
        body = "Nothing has been saved for this screen yet, so there is nothing to show.",
        modifier = modifier,
        action = { Button(onClick = onRetry) { Text("Try again") } },
    )
}

@Preview(name = "States light")
@Preview(name = "States dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatePreviews() {
    TodayFeedTheme {
        Column {
            LoadingState(Modifier)
            EmptyState(title = "Nothing saved yet")
            ErrorState(message = "Could not reach the server.", onRetry = {})
            OfflineState(onRetry = {})
        }
    }
}
